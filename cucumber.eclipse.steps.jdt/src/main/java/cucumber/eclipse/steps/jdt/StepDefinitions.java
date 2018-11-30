package cucumber.eclipse.steps.jdt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import cucumber.api.TypeRegistry;
import cucumber.api.TypeRegistryConfigurer;
import cucumber.eclipse.steps.integration.IStepListener;
import cucumber.eclipse.steps.integration.Step;
import cucumber.eclipse.steps.integration.StepsChangedEvent;
import cucumber.runtime.DefaultTypeRegistryConfiguration;
import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterByTypeTransformer;
import io.cucumber.cucumberexpressions.ParameterType;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import io.cucumber.cucumberexpressions.UndefinedParameterTypeException;
import io.cucumber.datatable.DataTableType;
import io.cucumber.datatable.TableCellByTypeTransformer;
import io.cucumber.datatable.TableEntryByTypeTransformer;

/*
 * Modified for Issue #211 : Duplicate Step definitions
 * Inheriting this class to cucumber.eclipse.editor.steps.jdt.JDTStepDefinitions child class
 * 
 */

//Commented for Issue #211 : Duplicate Step definitions
//public class StepDefinitions implements IStepDefinitions {

public class StepDefinitions extends MethodDefinition {

	private static final String MARKER_STEPPARSERPROBLEM = "cucumber.eclipse.steps.jdt.stepparserproblem";

	public static volatile StepDefinitions INSTANCE;

	private final Pattern cukeAnnotationMatcher = Pattern.compile("cucumber\\.api\\.java\\.([a-z_]+)\\.(.*)$");
	private static final String CUCUMBER_API_JAVA = "cucumber.api.java.";
	private static final String CUCUMBER_API_JAVA8 = "cucumber.api.java8.";

	public String JAVA_PROJECT = "org.eclipse.jdt.core.javanature";
	public int JAVA_SOURCE = IPackageFragmentRoot.K_SOURCE;
	public int JAVA_JAR_BINARY = IPackageFragmentRoot.K_BINARY;

	public String COMMA = ",";

	// public List<IStepListener> listeners = new ArrayList<IStepListener>();

	// #240:For Changes in step implementation is reflected in feature file
	protected static List<IStepListener> listeners = new ArrayList<IStepListener>();

	public StepDefinitions() {

	}

	/**
	 * Initialize
	 * 
	 * @return StepDefinitions
	 */
	public static StepDefinitions getInstance() {
		if (INSTANCE == null) {
			synchronized (StepDefinitions.class) {
				if (INSTANCE == null) {
					INSTANCE = new StepDefinitions();
				}
			}
		}

		return INSTANCE;
	}

	/*
	 * Commented due to Issue #211 : Duplicate Step definitions Redefined in
	 * cucumber.eclipse.editor.steps.jdt.JDTStepDefinitions child class
	 */
	// 1. To get Steps as Set from java file
	/*
	 * @Override public Set<Step> getSteps(IFile featurefile) {
	 * 
	 * Set<Step> steps = new LinkedHashSet<Step>();
	 * 
	 * IProject project = featurefile.getProject(); try { if
	 * (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) { IJavaProject
	 * javaProject = JavaCore.create(project);
	 * 
	 * //Issue #211 : Duplicate Step definitions List<IJavaProject> projectsToScan =
	 * new ArrayList<IJavaProject>(); projectsToScan.add(javaProject);
	 * projectsToScan.addAll(getRequiredJavaProjects(javaProject));
	 * 
	 * for (IJavaProject currentJavaProject: projectsToScan) {
	 * scanJavaProjectForStepDefinitions(currentJavaProject, steps); } } } catch
	 * (CoreException e) { e.printStackTrace(); }
	 * 
	 * return steps; }
	 */

	// From Java-Source-File(.java) : Collect All Steps as List based on
	// Cucumber-Annotations
	public List<Step> getCukeSteps(IJavaProject javaProject, ICompilationUnit iCompUnit,Collection<TypeRegistryConfigurer> typeRegistryConfigurers,
			IProgressMonitor progressMonitor) throws JavaModelException, CoreException {
		List<Step> steps = new ArrayList<Step>();
		List<CucumberAnnotation> importedAnnotations = new ArrayList<CucumberAnnotation>();
		IImportDeclaration[] allimports = iCompUnit.getImports();

		for (IImportDeclaration decl : allimports) {

			// Match Package name
			Matcher m = cukeAnnotationMatcher.matcher(decl.getElementName());
			if (m.find()) {
				if ("*".equals(m.group(2))) {
					importedAnnotations.addAll(
							getAllAnnotationsInPackage(javaProject, CUCUMBER_API_JAVA + m.group(1), m.group(1)));
				} else {
					importedAnnotations.add(new CucumberAnnotation(m.group(2), m.group(1)));
				}
			}

			// If import declaration matches with 'cucumber.api.java8'
			// Then set Language of Java8-cuke-api
			if (decl.getElementName().matches(REGEX_JAVA8_CUKEAPI)) {
				String importDeclaration = decl.getElementName();
				setJava8CukeLang(importDeclaration);
			}
		}
		Map<String, Locale> localeCache = new HashMap<String, Locale>();
		Map<Locale, ExpressionFactory> expressionFactoryCache = new HashMap<Locale, ExpressionFactory>();

		List<MethodDeclaration> methodDeclList = null;
		JavaParser javaParser = null;
		for (IType t : iCompUnit.getTypes()) {
			IResource resource = iCompUnit.getResource();
			resource.deleteMarkers(MARKER_STEPPARSERPROBLEM, false, 1);
			// collect all steps from java8 lamdas
			for (IType ifType : t.newTypeHierarchy(progressMonitor).getAllInterfaces()) {

				if (ifType.isInterface() && ifType.getFullyQualifiedName().startsWith(CUCUMBER_API_JAVA8)) {
					String[] superInterfaceNames = ifType.getSuperInterfaceNames();
					for (String superIfName : superInterfaceNames) {
						if (superIfName.endsWith(".LambdaGlueBase")) {
							// we found a possible interface, now try to load the language...
							String lang = ifType.getElementName().toLowerCase();
							// init if not done in previous step..
							if (javaParser == null) {
								javaParser = new JavaParser(iCompUnit, progressMonitor);
							}
							if (methodDeclList == null) {
								methodDeclList = javaParser.getAllMethods();
							}
							Set<String> keyWords = new HashSet<String>();
							for (IMethod method : ifType.getMethods()) {
								keyWords.add(method.getElementName());
							}
							List<MethodDefinition> methodDefList = new ArrayList<MethodDefinition>();
							// Visiting Methods/Constructors
							for (MethodDeclaration method : methodDeclList) {

								// Get Method/Constructor-Block{...}
								if (isCukeLambdaExpr(method, keyWords)) {
									// Collect method-body as List of Statements
									@SuppressWarnings("unchecked")
									List<Statement> statementList = method.getBody().statements();
									if (!statementList.isEmpty()) {
										MethodDefinition definition = new MethodDefinition(method.getName(),
												method.getReturnType2(), statementList);
										methodDefList.add(definition);
										definition.setJava8CukeLang(lang);
									}
								}
							}
							// Iterate MethodDefinition
							for (MethodDefinition method : methodDefList) {
								// Iterate Method-Statements
								for (Statement statement : method.getMethodBodyList()) {
									int line = -1;
									try {
										line = javaParser.getLineNumber(statement);
										String lambdaStep = method.getLambdaStep(statement, keyWords);
										if (lambdaStep == null) {
											continue;
										}
										Locale locale = getOrCreateLocale(localeCache, method.getCukeLang());
										ExpressionFactory expressionFactory = getOrCreateExpressionFactory(
												expressionFactoryCache, locale,typeRegistryConfigurers);
										Expression expression = expressionFactory.createExpression(lambdaStep);
										steps.add(new Step(expression, line, resource));
									} catch (PatternSyntaxException e) {
										reportError(resource, line, "Can't parse pattern " + e.getPattern()
												+ ", problem: " + e.getDescription());
									} catch (UndefinedParameterTypeException e) {
										reportError(resource, line, "Undefined parameter type " + e.getMessage());
									}

								}
							}
						}
					}
				}
			}
			// Collect all steps from Annotations used in the methods as per imported
			// Annotations
			for (IMethod method : t.getMethods()) {
				for (IAnnotation annotation : method.getAnnotations()) {
					CucumberAnnotation cukeAnnotation = getCukeAnnotation(importedAnnotations, annotation);
					if (cukeAnnotation != null) {
						int line = -1;
						try {
							line = getLineNumber(iCompUnit, annotation);
							Locale locale = getOrCreateLocale(localeCache, cukeAnnotation.getLang());
							ExpressionFactory expressionFactory = getOrCreateExpressionFactory(expressionFactoryCache,
									locale,typeRegistryConfigurers);
							Expression expression = expressionFactory.createExpression(getAnnotationText(annotation));
							steps.add(new Step(expression, line, method.getResource()));
						} catch (PatternSyntaxException e) {
							reportError(resource, line,
									"Can't parse pattern " + e.getPattern() + ", problem: " + e.getDescription());
						} catch (UndefinedParameterTypeException e) {
							reportError(resource, line, "Undefined parameter type " + e.getMessage());
						} catch (RuntimeException e) {
							reportError(resource, line,
								"Can't parse step definition "+e);
							
						}
					}
				}

			}
		}
		return steps;
	}

	public static Collection<TypeRegistryConfigurer> loadTypeRegistryConfigurerFromProject(IJavaProject javaProject,
			IProgressMonitor progressMonitor) throws JavaModelException, CoreException {
		Collection<TypeRegistryConfigurer> typeRegistryConfigurers = new ArrayList<TypeRegistryConfigurer>();
		IType type = javaProject.findType(TypeRegistryConfigurer.class.getName(), progressMonitor);
		if (type != null) {
			ITypeHierarchy hierarchy = type.newTypeHierarchy(progressMonitor);
			IType[] types = hierarchy.getImplementingClasses(type);
			String[] classPath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
			List<URL> urlList = new ArrayList<URL>();
			for (String entry : classPath) {
				IPath path = new Path(entry);
				try {
					URL url = path.toFile().toURI().toURL();
					urlList.add(url);
				} catch (MalformedURLException e) {
					//can't use it then...
				}
			}
			URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
			URLClassLoader classLoader = new URLClassLoader(urls, StepDefinitions.class.getClassLoader());
			for (IType iType : types) {
				String qualifiedName = iType.getFullyQualifiedName();
				if (DefaultTypeRegistryConfiguration.class.getName().equals(qualifiedName)) {
					continue;
				}
				try {
					Class<?> loadClass = classLoader.loadClass(qualifiedName);
					typeRegistryConfigurers.add((TypeRegistryConfigurer) loadClass.newInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				classLoader.close();
			} catch (IOException e) {
				//we don't care then...
			}
		}
		return typeRegistryConfigurers;
	}

	void reportError(IResource resource, int line, String msg) {
		if (resource == null) {
			System.err.println("Can't report error, resource is null! (" + msg + ", line " + line + ")");
		}
		try {
			IMarker m = resource.createMarker(MARKER_STEPPARSERPROBLEM);
			m.setAttribute(IMarker.LINE_NUMBER, line);
			m.setAttribute(IMarker.MESSAGE, msg);
			m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
			m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		} catch (CoreException e) {
			System.err.println("Can't report error, create marker failed! (" + msg + ", line " + line + ")");
			e.printStackTrace();
		}
	}

	public static ExpressionFactory getOrCreateExpressionFactory(Map<Locale, ExpressionFactory> expressionFactoryCache,
			Locale locale, Collection<TypeRegistryConfigurer> typeRegistryConfigurers) {
		if (expressionFactoryCache != null) {
			ExpressionFactory expressionFactory = expressionFactoryCache.get(locale);
			if (expressionFactory != null) {
				return expressionFactory;
			}
		}
		final ParameterTypeRegistry typeRegistry = new ParameterTypeRegistry(locale);
		
		ExpressionFactory factory = new ExpressionFactory(typeRegistry);
		if (typeRegistryConfigurers!=null) {
			for (TypeRegistryConfigurer typeRegistryConfigurer : typeRegistryConfigurers) {
				cucumber.api.TypeRegistry xx=new cucumber.api.TypeRegistry() {

					@Override
					public void defineDataTableType(DataTableType arg0) {
						
					}

					@Override
					public void defineParameterType(ParameterType<?> parameterType) {
						typeRegistry.defineParameterType(parameterType);
					}

					@Override
					public void setDefaultDataTableCellTransformer(TableCellByTypeTransformer arg0) {
						
					}

					@Override
					public void setDefaultDataTableEntryTransformer(TableEntryByTypeTransformer arg0) {
						
					}

					@Override
					public void setDefaultParameterTransformer(ParameterByTypeTransformer arg0) {
						
					}
					
				};
				typeRegistryConfigurer.configureTypeRegistry(xx);
			}
		}
		if (expressionFactoryCache != null) {
			expressionFactoryCache.put(locale, factory);
		}
		return factory;

	}

	public static Locale getOrCreateLocale(Map<String, Locale> localeCache, String cukeLang) {
		if (cukeLang == null) {
			return Locale.getDefault();
		}
		if (localeCache != null) {
			Locale locale = localeCache.get(cukeLang);
			if (locale != null) {
				return locale;
			}
		}
		Locale newLocale = new Locale(cukeLang);
		if (localeCache != null) {
			localeCache.put(cukeLang, newLocale);
		}
		return newLocale;
	}

	/**
	 * From JAR-File(.class) : Collect All Steps as List based on
	 * Cucumber-Annotations
	 * 
	 * @param javaPackage
	 * @param classFile
	 * @param localeCache
	 * @param expressionFactoryCache
	 * @param configurer 
	 * @return List<Step>
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public List<Step> getCukeSteps(IPackageFragment javaPackage, IClassFile classFile, Map<String, Locale> localeCache,
			Map<Locale, ExpressionFactory> expressionFactoryCache, Collection<TypeRegistryConfigurer> configurer) throws JavaModelException, CoreException {

		List<Step> steps = new ArrayList<Step>();
		List<CucumberAnnotation> importedAnnotations = new ArrayList<CucumberAnnotation>();

		// Get content as children
		for (IJavaElement javaElement : classFile.getChildren()) {
			IResource resource = javaElement.getResource();
			if (javaElement instanceof IType) {

				// System.out.println("--------IType "
				// +javaElement.getElementName());
				/*
				 * IInitializer IInitializer[] inits = ((IType) javaElement).getInitializers();
				 * for (IInitializer init : inits) {
				 * System.out.println("----------------IInitializer: "+ init.getElementName());
				 * } IField IField[] fields = ((IType)javaElement).getFields(); for (IField
				 * field : fields) { System.out.println("----------------field: "
				 * +field.getElementName()); }
				 */

				// IMethod
				IMethod[] methods = ((IType) javaElement).getMethods();
				for (IMethod method : methods) {
					// System.out.println("----------------method-name :
					// "+method.getElementName());
					// System.out.println("----------------method return type :
					// "+method.getReturnType());
					// System.out.println("----------------method-source :
					// +classFile.getElementName());

					for (IAnnotation annotation : method.getAnnotations()) {
						CucumberAnnotation cukeAnnotation = getCukeAnnotation(importedAnnotations, annotation);
						if (cukeAnnotation != null) {
							try {
								Locale locale = getOrCreateLocale(localeCache, cukeAnnotation.getLang());
								ExpressionFactory expressionFactory = getOrCreateExpressionFactory(
										expressionFactoryCache, locale,configurer);
								Expression expression = expressionFactory
										.createExpression(getAnnotationText(annotation));
								steps.add(
										new Step(expression, classFile.getElementName(), javaPackage.getElementName()));
							} catch (PatternSyntaxException e) {
								reportError(resource, -1,
										"Can't parse pattern " + e.getPattern() + ", problem: " + e.getDescription());
							} catch (UndefinedParameterTypeException e) {
								reportError(resource, -1, "Undefined parameter type " + e.getMessage());
							}
						}
					}
				}
			}
		}

		return steps;
	}

	/**
	 * @param compUnit
	 * @param annotation
	 * @return int
	 * @throws JavaModelException
	 */
	public int getLineNumber(ICompilationUnit compUnit, IAnnotation annotation) throws JavaModelException {
		Document document = new Document(compUnit.getBuffer().getContents());

		try {
			return document.getLineOfOffset(annotation.getSourceRange().getOffset()) + 1;
		} catch (BadLocationException e) {
			return 0;
		}
	}

	/**
	 * @param javaProject
	 * @param packageFrag
	 * @param lang
	 * @return List<CucumberAnnotation>
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	public List<CucumberAnnotation> getAllAnnotationsInPackage(final IJavaProject javaProject, final String packageFrag,
			final String lang) throws CoreException, JavaModelException {

		SearchPattern pattern = SearchPattern.createPattern(packageFrag, IJavaSearchConstants.PACKAGE,
				IJavaSearchConstants.IMPORT_DECLARATION_TYPE_REFERENCE, SearchPattern.R_EXACT_MATCH);

		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(javaProject.getPackageFragments());

		final List<CucumberAnnotation> annotations = new ArrayList<CucumberAnnotation>();

		SearchRequestor requestor = new SearchRequestor() {
			public void acceptSearchMatch(SearchMatch match) {
				try {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE) {
						IPackageFragment frag = (IPackageFragment) match.getElement();
						for (IClassFile cls : frag.getClassFiles()) {
							IType t = cls.findPrimaryType();
							if (t.isAnnotation()) {
								annotations.add(new CucumberAnnotation(t.getElementName(), lang));
							}
						}
					}
				} catch (JavaModelException e) {
				}
			}
		};
		SearchEngine engine = new SearchEngine();
		engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope, requestor,
				null);

		return annotations;
	}

	/**
	 * @param importedAnnotations
	 * @param annotation
	 * @return CucumberAnnotation
	 * @throws JavaModelException
	 */
	public CucumberAnnotation getCukeAnnotation(List<CucumberAnnotation> importedAnnotations, IAnnotation annotation)
			throws JavaModelException {

		Matcher m = cukeAnnotationMatcher.matcher(annotation.getElementName());
		if (m.find()) {
			return new CucumberAnnotation(m.group(2), m.group(1));
		}
		for (CucumberAnnotation cuke : importedAnnotations) {
			if (cuke.getAnnotation().equals(annotation.getElementName()))
				return cuke;
		}
		return null;
	}

	/**
	 * @param annotation
	 * @return String
	 * @throws JavaModelException
	 */
	public String getAnnotationText(IAnnotation annotation) throws JavaModelException {
		for (IMemberValuePair mvp : annotation.getMemberValuePairs()) {
			if (mvp.getValueKind() == IMemberValuePair.K_STRING) {
				return (String) mvp.getValue();
			}
		}
		return "";
	}

	/**
	 * @param javaProject
	 * @return List<IJavaProject>
	 * @throws CoreException
	 */
	public static List<IJavaProject> getRequiredJavaProjects(IJavaProject javaProject) throws CoreException {

		List<String> requiredProjectNames = Arrays.asList(javaProject.getRequiredProjectNames());

		List<IJavaProject> requiredProjects = new ArrayList<IJavaProject>();

		for (String requiredProjectName : requiredProjectNames) {

			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(requiredProjectName);

			if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {

				requiredProjects.add(JavaCore.create(project));
			}
		}
		return requiredProjects;
	}

	/**
	 * @param projectToScan
	 * @param collectedSteps
	 * @param progressMonitor
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public void scanJavaProjectForStepDefinitions(IJavaProject projectToScan, Collection<Step> collectedSteps,
			IProgressMonitor progressMonitor) throws JavaModelException, CoreException {
		IPackageFragment[] packages = projectToScan.getPackageFragments();
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor, packages.length+1);
		Collection<TypeRegistryConfigurer> configurer = loadTypeRegistryConfigurerFromProject(projectToScan, subMonitor.newChild(1));
		for (IPackageFragment javaPackage : packages) {

			if (javaPackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				SubMonitor child = subMonitor.newChild(1);
				ICompilationUnit[] compilationUnits = javaPackage.getCompilationUnits();
				child.setWorkRemaining(compilationUnits.length);
				for (ICompilationUnit compUnit : compilationUnits) {
					collectedSteps.addAll(getCukeSteps(projectToScan, compUnit, configurer, child.newChild(1)));
				}
			}
		}
	}

	/*
	 * Commented due to Issue #211 : Duplicate Step definitions Redefined in
	 * 'cucumber.eclipse.editor.steps.jdt.JDTStepDefinitions' child class
	 */
	/*
	 * @Override public void addStepListener(IStepListener listener) {
	 * this.listeners.add(listener); }
	 */

	public void removeStepListener(IStepListener listener) {
		listeners.remove(listener);
	}

	public void notifyListeners(StepsChangedEvent event) {
		for (IStepListener listener : listeners) {
			listener.onStepsChanged(event);
		}
	}

}