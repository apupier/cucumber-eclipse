package cucumber.eclipse.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.pde.ui.launcher.AbstractLaunchShortcut;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

public class CucumberFeatureLaunchShortcut extends AbstractLaunchShortcut implements ILaunchShortcut2 {

	private String newLaunchConfigurationName;
	private IProject project;
	private IResource selectedResource;
	
	/**
	 * This method is called when the user do a right clic and Run as cucumber feature
	 * on a feature file.
	 */
	@Override
	public void launch(ISelection selection, String mode) {
		this.project = (IProject) this.getLaunchableResource(selection);
		launch(mode);
	}

  	@Override
	public void launch(IEditorPart part, String mode) {
  		newLaunchConfigurationName = part.getTitle();
  		this.project = (IProject) this.getLaunchableResource(part);
		launch(mode);
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		if(selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection) selection;
			Object o = treeSelection.getFirstElement();
			if(o instanceof IResource) {
				IResource selectedResource = (IResource) o;
				this.selectedResource = selectedResource;
				return selectedResource.getProject();
			}
			else if (o instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) o;
				this.selectedResource = packageFragmentRoot.getResource(); 
				return this.selectedResource.getProject();
			}
		}
		return null;
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editor) {
		IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
		IFile file = input.getFile();
		this.selectedResource = file;
		return file;
//		IProject activeProject = file.getProject();
//		return activeProject;
	}

	@Override
	protected String getLaunchConfigurationTypeName() {
		return CucumberFeatureLaunchConstants.CUCUMBER_FEATURE_LAUNCH_CONFIG_TYPE;
	}

	@Override
	protected void initializeConfiguration(ILaunchConfigurationWorkingCopy config) {
//		IProject project = CucumberFeatureLaunchUtils.getProject();

		String featuresLocation = this.selectedResource.getLocation().toString();
		
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_FEATURE_PATH, featuresLocation);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_GLUE_PATH, CucumberFeatureLaunchConstants.DEFAULT_CLASSPATH);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_MONOCHROME, true);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_PRETTY, true);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_HTML, false);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_PROGRESS, false);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_JSON, false);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_JUNIT, false);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_RERUN, false);
		config.setAttribute(CucumberFeatureLaunchConstants.ATTR_IS_USAGE, false);
		
	}
	
	@Override
	protected String getName(ILaunchConfigurationType type) {
		if(newLaunchConfigurationName != null) {
			return newLaunchConfigurationName;
		}
		return super.getName(type);
	}

	
	@Override
	protected boolean isGoodMatch(ILaunchConfiguration configuration) {
		boolean goodType = isGoodType(configuration);
		boolean goodName = isGoodName(configuration);
		return goodType && goodName;
	}

	private boolean isGoodName(ILaunchConfiguration configuration) {
		return configuration.getName().equals(newLaunchConfigurationName);
	}

	private boolean isGoodType(ILaunchConfiguration configuration) {
		try {
			String identifier = configuration.getType().getIdentifier();
			return CucumberFeatureLaunchConstants.CUCUMBER_FEATURE_LAUNCH_CONFIG_TYPE.equals(identifier);
		} catch (CoreException e) {
			return false;
		}
	}

}
