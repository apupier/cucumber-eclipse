package io.cucumber.eclipse.editor.codemining;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

public class TableRowCodeMining extends LineHeaderCodeMining {


	private static final String LABEL = " " + (char) 0x2295 + " Add ";
	private String name;

	public TableRowCodeMining(String name, Position position, ICodeMiningProvider provider)
            throws BadLocationException {
		super(position, provider, null);
		this.name = name;
    }

    @Override
    protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
        return CompletableFuture.runAsync(() -> {
			super.setLabel(LABEL + name + " ");
			setLabel(getLabel());
        });
    }

	@Override
	public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
		// TODO colors
		return super.draw(gc, textWidget, color, x, y);
	}
}