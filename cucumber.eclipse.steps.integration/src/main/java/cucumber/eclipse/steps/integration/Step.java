package cucumber.eclipse.steps.integration;

import java.util.List;

import org.eclipse.core.resources.IResource;

import io.cucumber.cucumberexpressions.Argument;
import io.cucumber.cucumberexpressions.Expression;

public class Step {

	private final IResource source;
	private final int lineNumber;
	private final Expression expression;

	// Added By Girija
	// For Reading Steps from External-ClassPath-JAR
	private final String sourceName;
	private final String packageName;

	/**
	 * Constructs a step from the given resource and expression, this is normaly the
	 * case for source files avaiable to java
	 * 
	 * @param expression
	 * @param lineNumber
	 * @param source
	 */
	public Step(Expression expression, int lineNumber, IResource source) {
		this(expression, lineNumber, source, null, null);
	}

	/**
	 * Used when the source is not avaiable (e.g. JAR resources)
	 * 
	 * @param expression
	 * @param sourceName
	 * @param packageName
	 */
	public Step(Expression expression, String sourceName, String packageName) {
		this(expression, -1, null, sourceName, packageName);
	}

	private Step(Expression expression, int lineNumber, IResource source, String sourceName, String packageName) {
		if (expression == null) {
			throw new IllegalArgumentException("Expression can't be null!");
		}
		this.expression = expression;
		this.lineNumber = lineNumber;
		this.source = source;
		this.sourceName = sourceName;
		this.packageName = packageName;
	}

	// public void setText(String text) {
	// this.text = text;
	// Locale locale = this.lang == null ? Locale.getDefault() : new
	// Locale(this.lang);
	// try {
	// this.expression = new ExpressionFactory(new
	// ParameterTypeRegistry(locale)).createExpression(text);
	// }
	//
	// catch (UndefinedParameterTypeException e) {
	// // the cucumber expression have a custom parameter type
	// // without definition.
	// // For example, "I have a {color} ball"
	// // But the "color" parameter type was not register
	// // thanks to a TypeRegistryConfigurer.
	// this.expression = null;
	// }
	// catch (PatternSyntaxException e) {
	// // This fix #286
	// // the regular expression is wrong
	// // we do not expect to match something with it
	// // but we do not want to crash the F3
	// this.expression = null;
	// }
	// }
	public IResource getSource() {
		return source;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * @deprecated use the Expresion directly to support a more rich feature set!
	 * @param s
	 * @return <code>true</code> if matched, <code>false</code> otherwise
	 */
	@Deprecated
	public boolean matches(String s) {
		List<Argument<?>> match = this.expression.match(s);
		return match != null;
	}
	
	public Expression getExpression() {
		return expression;
	}

	/**
	 * 
	 * @return the name of the source
	 */
	public String getSourceName() {
		if (source != null) {
			return source.getName();
		}
		return sourceName;
	}

	/**
	 * 
	 * @return the package name of the source (if avaiable)
	 */
	public String getPackageName() {
		if (source != null) {
			// FIXME get the package name from the source!?
		}
		return packageName;
	}

	@Override
	public String toString() {

		if (lineNumber != 0) {
			return "Step [text=" + getText() + ", source=" + source + ", lineNumber=" + lineNumber + "]";
		} else {
			return "Step [text=" + getText() + ", source=" + sourceName + ", package=" + packageName + "]";
		}
	}

	/**
	 * @deprecated use {@link #getExpression()}.getSource() instead
	 * @return
	 */
	@Deprecated
	public String getText() {
		return expression.getSource();
	}

}
