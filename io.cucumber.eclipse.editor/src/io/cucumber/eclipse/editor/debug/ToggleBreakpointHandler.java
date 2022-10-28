package io.cucumber.eclipse.editor.debug;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

public class ToggleBreakpointHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object applicationContext = event.getApplicationContext();
		Map parameters = event.getParameters();
		Object trigger = event.getTrigger();
		// TODO Auto-generated method stub
		return null;
	}

}
