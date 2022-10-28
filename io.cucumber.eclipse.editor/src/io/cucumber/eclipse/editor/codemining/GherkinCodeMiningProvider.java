package io.cucumber.eclipse.editor.codemining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

import io.cucumber.eclipse.editor.document.GherkinEditorDocument;

public class GherkinCodeMiningProvider implements ICodeMiningProvider {

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		return CompletableFuture.supplyAsync(() -> {
			IDocument document = viewer.getDocument();
			GherkinEditorDocument editorDocument = GherkinEditorDocument.get(document);
			if (editorDocument == null) {
				return Collections.emptyList();
			}
			List<ICodeMining> list = new ArrayList<>();
//			StringBuilder sb = new StringBuilder("==================\r\n");
//			editorDocument.getTableHeaders().forEach(header -> {
//				sb.append("------------Table Header-------------\r\n");
//				for (TableCell cell : header.getCellsList()) {
//					sb.append(cell.getValue() + " | ");
//				}
//				sb.append("\r\n");
//				try {
//					list.add(new TableHeaderCodeMining(editorDocument.getEolPosition(header.getLocation()),
//							GherkinCodeMiningProvider.this));
//				} catch (BadLocationException e) {
//					e.printStackTrace();
//				}
//			});
//			editorDocument.getTableBodys().forEach(body -> {
//				int size = body.size();
//				if (size > 0) {
//					TableRow lastRow = body.get(size - 1);
//					try {
//						// TODO use name of the gherking dialect here?
//						list.add(new TableRowCodeMining("Example",
//								editorDocument.getEolPosition(lastRow.getLocation()),
//								GherkinCodeMiningProvider.this));
//					} catch (BadLocationException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			});
//			editorDocument.getDataTables().forEach(t -> {
//				sb.append("---------Datatable--------[" + t.getLocation().getLine() + " " + System.identityHashCode(t)
//						+ "]---------\r\n");
////				System.out.println(t);
//				List<TableRow> rowsList = t.getRowsList();
//				for (TableRow tableRow : rowsList) {
//					for (TableCell cell : tableRow.getCellsList()) {
//						sb.append(cell.getValue() + " | ");
//					}
//					sb.append("\r\n");
//				}
//				try {
//					list.add(new TableHeaderCodeMining(editorDocument.getEolPosition(t.getLocation()),
//							GherkinCodeMiningProvider.this));
//					if (rowsList.size() > 1) {
//						TableRow lastRow = rowsList.get(rowsList.size() - 1);
//						Position position = editorDocument.getPosition(lastRow.getLocation(), -1);
//						list.add(new TableRowCodeMining("Data", position,
//								GherkinCodeMiningProvider.this));
//					}
//				} catch (BadLocationException e) {
//					e.printStackTrace();
//				}
//			});
//			System.out.println(sb);
			return list;
		});
	}


	@Override
	public void dispose() {
	}

}
