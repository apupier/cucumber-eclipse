package io.cucumber.eclipse.java.launching;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jface.text.BadLocationException;

import io.cucumber.eclipse.editor.debug.GherkingBreakpoint;
import io.cucumber.eclipse.editor.debug.GherkingDebugTarget;
import io.cucumber.eclipse.editor.debug.GherkingStepStackFrame;
import io.cucumber.eclipse.editor.debug.GherkingThread;
import io.cucumber.eclipse.editor.document.GherkinMessageHandler;
import io.cucumber.eclipse.editor.document.TestStepEvent;
import io.cucumber.eclipse.java.JDTUtil;
import io.cucumber.eclipse.java.plugins.CucumberCodeLocation;
import io.cucumber.messages.Messages.SourceReference;
import io.cucumber.messages.Messages.SourceReference.JavaMethod;
import io.cucumber.messages.Messages.StepDefinition;

public class CucumberDebugTarget extends GherkingDebugTarget<MessageEndpointProcess> {

	private volatile boolean suspendOnNextStep;
	private IBreakpoint[] breakpoints;
	private IJavaProject javaProject;

	public CucumberDebugTarget(ILaunch launch, MessageEndpointProcess endpoint, IJavaProject javaProject) {
		super(launch, endpoint, "cucumber-jvm");
		this.javaProject = javaProject;
		breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(GherkingBreakpoint.MODEL_ID);
		endpoint.addEnvelopeListener(new GherkinMessageHandler() {

			@Override
			protected void handleTestStepStart(TestStepEvent event) {
				testStepStart(event);
			}
			
		});
	}

	protected void testStepStart(TestStepEvent event) {
		try {
			if (suspendOnNextStep) {
				suspendOnNextStep = false;
				GherkingThread thread = getThread();
				thread.suspend(trace(event, thread), DebugEvent.STEP_OVER).await();
				return;
			}
			for (IBreakpoint breakpoint : breakpoints) {
				if (breakpoint instanceof GherkingBreakpoint) {
					GherkingBreakpoint gbp = (GherkingBreakpoint) breakpoint;
					if (gbp.getLineNumber() == event.getStep().getLocation().getLine()) {
						GherkingThread thread = getThread();
						thread.suspend(gbp, trace(event, thread)).await();
						return;
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private IStackFrame[] trace(TestStepEvent event, GherkingThread thread) {
		IStackFrame[] stackFrames = event.getStackTrace(thread);
		for (IStackFrame frame : stackFrames) {
			if (frame instanceof GherkingStepStackFrame) {
				GherkingStepStackFrame stepStackFrame = (GherkingStepStackFrame) frame;
				stepStackFrame.setStepOverHandler(() -> {
					suspendOnNextStep = true;
					thread.resume();
				});
				StepDefinition stepDefinition = stepStackFrame.getStepDefinition();
				if (stepDefinition != null && stepDefinition.hasSourceReference()) {
					SourceReference ref = stepDefinition.getSourceReference();
					if (ref.hasJavaMethod()) {
						JavaMethod method = ref.getJavaMethod();
						CucumberCodeLocation location = new CucumberCodeLocation(method.getClassName(),
								method.getMethodName(), method.getMethodParameterTypesList().toArray(String[]::new));
						stepStackFrame.setStepIntoHandler(() -> {
							// see org.eclipse.jdt.internal.debug.ui.actions.RunToLineAdapter
							// see org.eclipse.debug.ui.actions.RunToLineHandler
							Job job = new Job("Step Into") {

								@Override
								protected IStatus run(IProgressMonitor monitor) {
									try {
										IMethod[] resolveMethod = JDTUtil.resolveMethod(javaProject, location, monitor);
										if (resolveMethod == null || resolveMethod.length == 0) {
											return Status.CANCEL_STATUS;
										}
										for (IMethod method : resolveMethod) {
											System.out.println("Step into " + method);
											Map<String, Object> map = new HashMap<>();
											BreakpointUtils.addJavaBreakpointAttributes(map, method);
											IResource resource = BreakpointUtils.getBreakpointResource(method);
											int range[] = JDTUtil.getLineNumberAndRange(method);
											IJavaMethodBreakpoint entryBreakpoint = JDIDebugModel
													.createMethodBreakpoint(
													resource, method.getDeclaringType().getFullyQualifiedName(),
													method.getElementName(), method.getSignature(), true, false, false,
													range[0], range[1], range[2], -1, false, map);
											// entryBreakpoint.set
										}
										return Status.OK_STATUS;
									} catch (CoreException e) {
										e.printStackTrace();
										return e.getStatus();
									} catch (BadLocationException e) {
										e.printStackTrace();
										return Status.CANCEL_STATUS;
									} finally {
										try {
											thread.resume();
										} catch (DebugException e) {
											e.printStackTrace();
										}
									}
								}
							};
							job.schedule();
						});
					}
				}

				IDebugTarget[] debugTargets = getLaunch().getDebugTargets();
				for (IDebugTarget target : debugTargets) {
					System.out.println(target);

					// new CucumberCodeLocation(definition.getLocation())

//					JDTUtil.resolveMethod(null, null, monitor)
//					Map<String, Object> map = new HashMap<>();
//					BreakpointUtils.addJavaBreakpointAttributes(map, fDestMethod);
//					IResource resource = BreakpointUtils.getBreakpointResource(fDestMethod);
//					int range[] = getNewLineNumberAndRange(fDestMethod);
//					map.put(IInternalDebugUIConstants.WORKING_SET_NAME, getOriginalWorkingSets());
//					IJavaMethodBreakpoint breakpoint = JDIDebugModel.createMethodBreakpoint(
//							resource,
//							fDestMethod.getDeclaringType().getFullyQualifiedName(),
//							fDestMethod.getElementName(),
//							fDestMethod.getSignature(),
//							isEntry(),
//							isExit(),
//							isNativeOnly(),
//							NO_LINE_NUMBER,
//							range[1],
//							range[2],
//							getHitCount(),
//							true,
//							map);
//					apply(breakpoint);
//					getOriginalBreakpoint().delete();
//					return new DeleteBreakpointChange(breakpoint);
				}
			}
		}
		return stackFrames;
	}



}
