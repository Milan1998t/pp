package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.ac.bg.etf.pp1.ast.FormListIskaz;
import rs.ac.bg.etf.pp1.ast.VarDeclIskaz;

public class CounterVisitor extends VisitorAdaptor {

	protected int count;

	public int getCount() {
		return count;
	}

	public static class FormParamCounter extends CounterVisitor {

		public void visit(FormListIskaz FormListIskaz) {
			count++;
		}
	}

	public static class VarCounter extends CounterVisitor {

		public void visit(VarDeclIskaz VarDeclIskaz) {
			count++;
		}
	}
}
