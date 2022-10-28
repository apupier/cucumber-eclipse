package io.cucumber.eclipse.editor.debug;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import io.cucumber.messages.Messages.GherkinDocument.Feature.Step;
import io.cucumber.messages.Messages.StepDefinition;
import io.cucumber.messages.Messages.TestCase.TestStep;

/**
 * Special stackframe that represents a test-step
 * 
 * @author christoph
 *
 */
public class GherkingStepStackFrame extends GherkingStackFrame {

	private Step step;
	private StepDefinition stepDefinition;
	private TestStep testStep;
	private DebugRunnable stepOverHandler;
	private DebugRunnable stepIntoHandler;

	public GherkingStepStackFrame(IThread thread, TestStep testStep, Step step, StepDefinition stepDefinition) {
		super(thread, step.getLocation().getLine(), "[" + step.getKeyword().strip() + "] " + step.getText());
		this.testStep = testStep;
		this.step = step;
		this.stepDefinition = stepDefinition;
		if (stepDefinition != null) {
			System.out.println(stepDefinition);
			GherkingStepDefinitionValue value = new GherkingStepDefinitionValue(this, stepDefinition, step.getText());
			addVariable(new GherkingStepVariable(this, stepDefinition.getPattern().getSource(), value));
			addGroups(this, value::addVariable);
		} else {
			addGroups(this, this::addVariable);
		}
	}

	private void addGroups(GherkingStackFrame stepFrame, Consumer<IVariable> variableConsumer) {
		AtomicInteger counter = new AtomicInteger();
		testStep.getStepMatchArgumentsListsList().stream().flatMap(list -> list.getStepMatchArgumentsList().stream())
				.forEach(argument -> {
					String type = argument.getParameterTypeName();
					variableConsumer.accept(new GherkingStepVariable(stepFrame, "arg" + counter.get(),
							new GherkingGroupValue(stepFrame.getDebugTarget(), type, argument.getGroup())));
				});
	}

	public Step getStep() {
		return step;
	}

	public StepDefinition getStepDefinition() {
		return stepDefinition;
	}

	public TestStep getTestStep() {
		return testStep;
	}

	public void setStepOverHandler(DebugRunnable runnable) {
		this.stepOverHandler = runnable;
	}

	@Override
	public boolean canStepOver() {
		return stepOverHandler != null;
	}

	@Override
	public void stepOver() throws DebugException {
		if (stepOverHandler != null) {
			stepOverHandler.run();
		}
	}

	public void setStepIntoHandler(DebugRunnable runnable) {
		this.stepIntoHandler = runnable;
	}

	@Override
	public boolean canStepInto() {
		return stepIntoHandler != null;
	}

	@Override
	public void stepInto() throws DebugException {
		if (stepIntoHandler != null) {
			stepIntoHandler.run();
		}
	}

}
