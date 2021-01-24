package io.cucumber.eclipse.java.validation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import io.cucumber.core.gherkin.FeatureParserException;
import io.cucumber.eclipse.editor.document.GherkinEditorDocument;
import io.cucumber.eclipse.editor.marker.MarkerFactory;
import io.cucumber.eclipse.java.JDTUtil;
import io.cucumber.eclipse.java.plugins.CucumberMatchedStepsPlugin;
import io.cucumber.eclipse.java.plugins.CucumberMissingStepsPlugin;
import io.cucumber.eclipse.java.plugins.CucumberStepParserPlugin;
import io.cucumber.eclipse.java.runtime.CucumberRuntime;
import io.cucumber.eclipse.java.steps.StepGenerator;

/**
 * Performs a dry-run on the document to verify step definition matching
 * 
 * @author christoph
 *
 */
public class CucumberGlueValidator implements IDocumentSetupParticipant {

	private static ConcurrentMap<IDocument, GlueJob> jobMap = new ConcurrentHashMap<>();

	@Override
	public void setup(IDocument document) {
		document.addDocumentListener(new IDocumentListener() {

			@Override
			public void documentChanged(DocumentEvent event) {
				// TODO configurable
				validate(document, 1000, false);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {

			}
		});
		validate(document, 0, false);
	}

	private void validate(IDocument document, int delay, boolean persistent) {
		jobMap.compute(document, (key, oldJob) -> {
			if (oldJob != null && !oldJob.persistent) {
				oldJob.cancel();
			}
			GlueJob verificationJob = new GlueJob(oldJob, document, persistent);
			verificationJob.setUser(false);
			verificationJob.setPriority(Job.DECORATE);
			if (delay > 0) {
				verificationJob.schedule(delay);
			} else {
				verificationJob.schedule();
			}
			return verificationJob;
		});

	}

	/**
	 * Allows to sync with the current glue code computation
	 * 
	 * @param document the document to sync on
	 * @param monitor  the progress monitor that can be used to cancel the join
	 *                 operation, or null if cancellation is not required. No
	 *                 progress is reported on this monitor.
	 * @throws OperationCanceledException on cancellation
	 * @throws InterruptedException       if the thread was interrupted while
	 *                                    waiting
	 */
	public static void sync(IDocument document, IProgressMonitor monitor)
			throws OperationCanceledException, InterruptedException {
		GlueJob glueJob = jobMap.get(document);
		if (glueJob != null) {
			glueJob.join(TimeUnit.SECONDS.toMillis(30), monitor);
		}
	}

	private static final class GlueJob extends Job {

		private GlueJob oldJob;
		private IDocument document;
		private boolean persistent;

		public GlueJob(GlueJob oldJob, IDocument document, boolean persistent) {
			super("Verify Cucumber Glue Code");
			this.oldJob = oldJob;
			this.document = document;
			this.persistent = persistent;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (oldJob != null) {
				try {
					oldJob.join();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return Status.CANCEL_STATUS;
				}
			}
			GherkinEditorDocument editorDocument = GherkinEditorDocument.get(document);
			if (editorDocument != null) {
				try {
					IResource resource = editorDocument.getResource();
					IJavaProject javaProject = JDTUtil.getJavaProject(resource);
					if (javaProject != null) {
						try (CucumberRuntime rt = CucumberRuntime.create(javaProject)) {
							rt.getRuntimeOptions().setDryRun();
							try {
								rt.addFeature(editorDocument);
							} catch (FeatureParserException e) {
								// the feature has syntax errors, we can't check the glue then...
								return Status.CANCEL_STATUS;
							}
							CucumberMissingStepsPlugin missingStepsPlugin = new CucumberMissingStepsPlugin();
							rt.addPlugin(new CucumberStepParserPlugin());
							rt.addPlugin(new CucumberMatchedStepsPlugin());
							rt.addPlugin(missingStepsPlugin);
							rt.run(monitor);
							MarkerFactory.missingSteps(resource, missingStepsPlugin.getSnippets(),
									StepGenerator.class.getName(), persistent);
						}
					}
				} catch (CoreException e) {
					return e.getStatus();
				}
			}
			jobMap.remove(document, this);
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

	}

}
