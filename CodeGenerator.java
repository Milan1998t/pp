package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;

public class CodeGenerator extends VisitorAdaptor {
	private int mainPc;
	Logger log = Logger.getLogger(getClass());
	public boolean plus = false;
	public boolean minus = false;
	public boolean mul = false;
	public boolean div = false;
	public boolean mod = false;
	public boolean inMethod = false;
	public Struct typeStruct = Tab.noType;
	static Struct boolType = new Struct(Struct.Bool);
	static Struct classType = new Struct(Struct.Class);
	public Struct exprType = Tab.noType;
	public int niz = 0;
	public int jesteNiz = 0;
	public Obj sacuvati = null;
	public Obj designator = null;
	public int novNiz = 0;
	public boolean neg = false;
	public int ispis = 5;
	public int constInt = -1;
	public char constChar = ',';
	public int constBoolean = 2;
	public int nizlevi = 0;
	public boolean newarr = false;
	public int ternarni = 0;
	public boolean truebool = false;

	public void report_error(String message, SyntaxNode info) {

		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.info(msg.toString());
	}

	public int getMainPc() {
		return mainPc;
	}

	public void visit(MethodDeclName MethodDeclName) {

		if (MethodDeclName.getMethodName().equals("main")) {
			mainPc = Code.pc;
		}
		// Tab.openScope();
		// MethodDeclName.obj.setAdr(Code.pc);
		// Collect arguments and local variables
		SyntaxNode methodNode = MethodDeclName.getParent();

		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);

		// Generate the entry
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(fpCnt.getCount() + varCnt.getCount());

		inMethod = true;

	}

	public void visit(ConstDeclTerm ConstDeclTerm) {

		if (constInt != -1) {
			Obj o = Tab.insert(Obj.Con, ConstDeclTerm.getConstName(), typeStruct);
			o.setAdr(constInt);
		}
		if (constChar != ',') {
			Obj o = Tab.insert(Obj.Con, ConstDeclTerm.getConstName(), typeStruct);
			o.setAdr(constChar);
		}
		if (constBoolean != 2) {
			Obj o = Tab.insert(Obj.Con, ConstDeclTerm.getConstName(), typeStruct);
			o.setAdr(1);
		}

		constInt = -1;
		constChar = ',';
		constBoolean = 2;
	}

	public void visit(AssingOpNumber AssingOpNumber) {
		constInt = AssingOpNumber.getNr();
	}

	public void visit(AssingOpChar AssingOpChar) {
		constChar = AssingOpChar.getCh();
	}

	public void visit(AssingOpBool AssingOpBool) {
		constBoolean = 1;
	}

	public void visit(PrintStatement PrintStatement) {
		if (haveMorePrints == true) {
			Code.loadConst(5);
			Code.put(Code.print);
		}
		if (ispis != 5) {
			if (ispis == 1 || ispis == 3) {
				Code.loadConst(5);
				Code.put(Code.print);
			}
			if (ispis == 2) {
				Code.loadConst(2);
				Code.put(Code.bprint);
			}
		}

		haveMorePrints = false;
		// report_error("ovde nece da pise", PrintStatement);

	}

	public void visit(FactorNewBrack FactorNewBrack) {
		int tip = 0;
		newarr = true;
		if (FactorNewBrack.getType().getTypeName().equals("int")) {
			Code.put(Code.newarray);
			Code.put(Code.const_1);
		} else {
			Code.put(Code.newarray);
			Code.loadConst(0);
		}
		// Code.load(sacuvati);

		novNiz = 1;
		sacuvati = null;

	}

	public void visit(FactorNumConst FactorNumConst) {
		exprType = new Struct(Struct.Int);
		Obj o = Tab.insert(Obj.Con, "$", new Struct(Struct.Int));
		o.setLevel(0);
		o.setAdr(FactorNumConst.getNr());
		// report_info("sta je provo"+FactorNumConst.getNr(), FactorNumConst);

		Code.load(o);
		/*
		 * if (neg == true) { // report_info("neg je", FactorNumConst);
		 * Code.loadConst(-1); Code.put(Code.mul); }
		 */
		// neg = false;
	}

	public void visit(Designator Designator) {
		// report_info("cekaj "+Designator.getVarName(), Designator);
		Obj o = Tab.find(Designator.getVarName());
		// report_info("on je ovo ustv"+Designator.getVarName(), Designator);
		// report_info("des obican "+Designator.getVarName(), Designator);
	}

	public void visit(HaveOptExpr HaveOptExpr) {
		// mozda ce morati druga promenjiva
		SyntaxNode sNode = HaveOptExpr.getExpr().getParent().getParent();
		if (sNode.getClass() == Designator.class) {
			// report_info("jeste niz exp", sNode);
			jesteNiz = 1;
			nizlevi = 1;
		} else {
			report_info("nije niz exp", sNode);
		}

	}

	public void visit(DsEqual DsEqual) {

		Obj o = Tab.find(DsEqual.getDesignator().getVarName());
		// report_info("equal "+DsEqual.getDesignator().getVarName()+jesteNiz, DsEqual);
		if (o.getType().getKind() == 5) {
			Code.store(o);
			return;
		}
		if (o.getType().getKind() == 3 && newarr == false) {
//			report_info("nizic" + o.getName(), DsEqual);
			jesteNiz = 1;
		}
		if (jesteNiz == 1) {
			// report_info("ovo je levi des"+DsEqual.getDesignator().getVarName(), DsEqual);
			Obj val = new Obj(Obj.Var, "value", new Struct(Struct.Int), 100, 1);
			val.setLevel(1);
			Code.store(val);
			Obj index = new Obj(Obj.Var, "index", new Struct(Struct.Int), 101, 1);
			index.setLevel(1);
			Code.store(index);
			Code.load(o);
			Code.load(index);
			Code.load(val);
			// Code.loadConst(5);
			// Code.put(Code.print);
			// Code.loadConst(5);
			// Code.put(Code.print);
			// Code.put(Code.astore);
			if (o.getType().getElemType().getKind() == 1) {

				Code.put(Code.astore);
			} else {
				Code.put(Code.astore);
			}
			// report_info("ovde", DsEqual);
			jesteNiz = 0;
			return;
		}
		if (nizlevi != 1) {
			// report_info("ili ovde", DsEqual);
			// Code.put(Code.const_1);
			Code.store(o);
			newarr = false;
			return;
		}
		newarr = false;
		Code.store(o);
	}

	public void visit(FactorDesignator FactorDesignator) {

		// report_info("factordes
		// "+FactorDesignator.getDesignator().getVarName(),FactorDesignator);

		Obj o = Tab.find(FactorDesignator.getDesignator().getVarName());

		if (o.getType().getKind() == 3) {
			if (o.getType().getElemType().getKind() == 1) {
				ispis = 1;
			}
			if (o.getType().getElemType().getKind() == 2) {
				ispis = 2;
			}
			if (o.getType().getElemType().getKind() == 5) {
				ispis = 3;
			}
		} else {
			if (o.getType().getKind() == 1) {
				ispis = 1;
			}
			if (o.getType().getKind() == 2) {
				ispis = 2;
			}
			if (o.getType().getKind() == 5) {
				ispis = 3;
			}
		}
		if (o.getKind() == 0) {
//			report_info("pronsao i " + o.getAdr(), FactorDesignator);
			jesteNiz = 0;
			Code.loadConst(o.getAdr());
			return;
		}
		if (o.getType().getKind() == 2) {
			report_error("char je " + o.getName() + o.getAdr(), FactorDesignator);
			Code.loadConst(o.getAdr());
			jesteNiz = 0;
			return;
		}
		if (o.getType().getKind() == 5) {
//			report_info("bool" + FactorDesignator.getDesignator().getVarName() + o.getAdr(), FactorDesignator);
			Code.load(o);
			jesteNiz = 0;
			return;
		}

		if (jesteNiz == 1) {
			// report_error("niz je"+FactorDesignator.getDesignator().getVarName(),
			// FactorDesignator);
			Obj tmp = new Obj(Obj.Var, "oo", new Struct(Struct.Int), 78, 1);

			tmp.setLevel(1);
			if(neg==true) {
				Code.put(Code.const_m1);
				Code.put(Code.mul);
			}
			Code.store(tmp);// ovde je sad konstanta

			Code.load(o);
			Code.load(tmp);
			// Code.loadConst(5);
			// Code.put(Code.print);
			Code.put(Code.aload);
			if(neg==true) {
				Code.put(Code.const_m1);
				Code.put(Code.mul);
			}
		} else {
			// report_error("nije niz je"+FactorDesignator.getDesignator().getVarName(),
			// FactorDesignator);
			if(neg==true) {
				Code.load(o);
				Code.put(Code.const_m1);
				Code.put(Code.mul);
			}else {
				Code.load(o);
			}
		}
		neg=false;
		jesteNiz = 0;
	}

	public void visit(MethodDecl methodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
		inMethod = false;
		int size = daSeVrateNaGlobalne.size();
		for (int i = 0; i < size; i++) {
			Obj da = daSeVrateNaGlobalne.get(i);
			da.setLevel(0);
			// report_info("stavljen nivo za glo" + da.getName() + " nivo "+da.getLevel(),
			// null);
		}
		// daSeVrateNaGlobalne.clear();
		// report_info("ovde", methodDecl);
	}

	public void visit(AddopTermListTerm addopTermListTerm) {
		Addop addop = addopTermListTerm.getAddop();
		if (addop instanceof AddopPlus) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		} /*
			 * if (plus == true) { Code.put(Code.add); } if (minus == true) {
			 * Code.put(Code.sub); }
			 */

//		neg = false;
	}
	boolean nextime=false;
	public void visit(Term Term) {
		/*
		 if(nextime==true) {
			 nextime=false;
			 neg=false;
		 }
		 if (neg == true) {
			 nextime=true;
		  Code.loadConst(-1); 
		  Code.put(Code.mul);
		  }
		  */
		 
//		  neg = false;
		
	}

	public void visit(HaveAddopTermLists haveAddopTermLists) {
		if(nextime==true) {
			 nextime=false;
			 neg=false;
		 }
		 if (neg == true) {
			 nextime=true;
		  Code.loadConst(-1); 
		  Code.put(Code.mul);
		  }
	}

	public void visit(DontHaveAddopTermLists DontHaveAddopTermLists) {
		if(nextime==true) {
			 nextime=false;
			 neg=false;
		 }
		 if (neg == true) {
			 nextime=true;
		  Code.loadConst(-1); 
		  Code.put(Code.mul);
		  }

//		neg = false;
	}

	public void visit(AddopTermListt addopTermListt) {
		Addop addop = addopTermListt.getAddop();
		if (addop instanceof AddopPlus) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		} /*
			 * if (plus == true) { Code.put(Code.add); } if (minus == true) {
			 * Code.put(Code.sub); }
			 */
	}

	public void visit(AddopPlus AddopPlus) {

		plus = true;
		minus = false;
	}

	public void visit(AddopMinus AddopMinus) {
		minus = true;
		plus = false;
	}

	public void visit(MulopFactorListTerm MulopFactorListTerm) {
		Mulop mulop= MulopFactorListTerm.getMulop();
		if(mulop instanceof MulopMul) {
			Code.put(Code.mul);
		}
		if(mulop instanceof MulopDiv) {
			Code.put(Code.div);
		}
		if(mulop instanceof MulopMod) {
			Code.put(Code.rem);
		}
		/*
		if (mul == true) {
			Code.put(Code.mul);
		}
		if (div == true) {
			Code.put(Code.div);
		}
		if (mod == true) {
			Code.put(Code.rem);
		}

		mod = false;
		div = false;
		mul = false;
		*/
	}

	public void visit(MulopFactorListt MulopFactorListt) {
		Mulop mulop= MulopFactorListt.getMulop();
		if(mulop instanceof MulopMul) {
			Code.put(Code.mul);
		}
		if(mulop instanceof MulopDiv) {
			Code.put(Code.div);
		}
		if(mulop instanceof MulopMod) {
			Code.put(Code.rem);
		}
		/*
		if (mul == true) {
			Code.put(Code.mul);
		}
		if (div == true) {
			Code.put(Code.div);
		}
		if (mod == true) {
			Code.put(Code.rem);
		}

		mod = false;
		div = false;
		mul = false;
		*/
	}

	public void visit(MulopMul MulopMul) {
		mul = true;
	}

	public void visit(MulopDiv MulopDiv) {
		div = true;
	}

	public void visit(MulopMod MulopMod) {
		mod = true;
	}

	public void visit(FactorCharConst FactorCharConst) {
		exprType = new Struct(Struct.Char);
		Obj o = Tab.insert(Obj.Con, "", Tab.charType);
		o.setAdr(FactorCharConst.getCh());
		// report_info("char"+o.getAdr(), FactorCharConst);
		Code.load(o);
	}

	public void visit(FactorBoolConsts FactorBoolConst) {
		exprType = new Struct(Struct.Bool);
		Obj o = Tab.insert(Obj.Con, "", boolType);
		/*
		 * if(FactorBoolConst.getBo().equals("true")) { o.setAdr(1); }else {
		 * o.setAdr(0); }
		 */
		// report_info("dosla bool", FactorBoolConst);
		if (FactorBoolConst.getBo().equals("true")) {
			o.setAdr(1);
			truebool = true;
		} else {
			o.setAdr(0);
			truebool = false;
		}

		Code.load(o);

	}

	public void visit(DsInc DsInc) {

		Obj o = Tab.find(DsInc.getDesignator().getVarName());
		if (o.getType().getKind() == 3) {
			// report_info("niz je u ++", DsInc);
			Obj tmp = new Obj(Obj.Var, "IncType", new Struct(Struct.Int), 99, 1);
			Code.store(tmp);
			Code.load(o);
			Code.load(tmp);
			Code.put(Code.dup2);
			Code.put(Code.aload);
			Code.put(Code.const_1);
			Code.put(Code.add);
			Code.put(Code.astore);
			return;

		}
		Code.put(Code.const_1);
		Code.load(o);
		Code.put(Code.add);
		Code.store(o);
		jesteNiz = 0;
	}

	public void visit(DsDec DsDec) {
		Obj o = Tab.find(DsDec.getDesignator().getVarName());
		if (o.getType().getKind() == 3) {
			// report_info("niz je u --", DsDec);
			Obj tmp = new Obj(Obj.Var, "DecType", new Struct(Struct.Int), 98, 1);
			Code.store(tmp);
			Code.load(o);
			Code.load(tmp);
			Code.put(Code.dup2);
			Code.put(Code.aload);
			Code.put(Code.const_1);
			Code.put(Code.sub);
			Code.put(Code.astore);
			return;

		}
		Code.load(o);
		Code.put(Code.const_1);
		Code.put(Code.sub);
		Code.store(o);
		jesteNiz = 0;
	}

	List<Obj> daSeVrateNaGlobalne = new ArrayList<>();

	public void visit(VarDeclIskaz VarDeclIskaz) {

		Obj o = Tab.find(VarDeclIskaz.getVarName());
		if (o == Tab.noObj || o == null) {
			if (niz == 1) {
				Struct pom = new Struct(3);
				pom.setElementType(typeStruct);
				Obj c = Tab.insert(Obj.Var, VarDeclIskaz.getVarName(), pom);
				if (inMethod == true) {
					c.setLevel(1);
				} else {
					c.setLevel(0);
					daSeVrateNaGlobalne.add(o);
					// report_info("deklarisana globalna niz " + c.getName() + " nivo " +
					// c.getLevel() + typeStruct.getKind(),VarDeclIskaz);
				}
				if (typeStruct == boolType) {
					// report_info("deklarisana bool", VarDeclIskaz);
				}
			} else {
				o = Tab.insert(Obj.Var, VarDeclIskaz.getVarName(), typeStruct);
				if (inMethod == true) {
					o.setLevel(1);
				} else {
					o.setLevel(0);
					daSeVrateNaGlobalne.add(o);
					// report_info("deklarisana globalna " + o.getName() + " nivo " + o.getLevel() +
					// typeStruct.getKind(),VarDeclIskaz);
				}
				if (typeStruct == boolType) {
					// report_info("deklarisana bool", VarDeclIskaz);
					if (truebool == true) {
						o.setAdr(1);
					} else {
						o.setAdr(0);
					}
					truebool = false;
					// report_info("deklarisana bool"+o.getAdr(), VarDeclIskaz);
				}
			}

		} else {
			if (inMethod = true || o.getLevel() == 0) {
				// report_info("sad je lokalna " + o.getName(), VarDeclIskaz);
				o.setLevel(1);
			}
		}
		niz = 0;
	}

	public void visit(Type type) {

		if (type.getTypeName().equals("bool")) {
			typeStruct = boolType;
		}
		if (type.getTypeName().equals("int")) {
			typeStruct = new Struct(Struct.Int);
		}
		if (type.getTypeName().equals("char")) {
			typeStruct = new Struct(Struct.Char);
		}

	}

	public void visit(BraceOpt BraceOpt) {
		niz = 1;
	}

	public void visit(HaveMinus HaveMinus) {
		//report_info("postavio", HaveMinus);
		neg = true;
	}

	public void visit(ReadStatement ReadStatement) {
		// report_error("" + ReadStatement.getDesignator().getVarName(), null);

		Obj o = Tab.find(ReadStatement.getDesignator().getVarName());
		if (o.getType().getKind() == 3) {
//			report_info("niz", ReadStatement);
			Obj tmp = new Obj(Obj.Var, "IncType", new Struct(Struct.Int), 99, 1);
			Code.store(tmp);
			Code.load(o);
			Code.load(tmp);
			Code.put(Code.read);
			Code.put(Code.astore);
			return;
		}
		Code.put(Code.read);
		Code.store(o);

	}

	public boolean pluspom = false;
	public boolean minuspom = false;
	public boolean mulpom = false;
	public boolean divpom = false;

	public void visit(FactorExpr FactorExpr) {

		plus = false;
		minus = false;
		mul = false;
		div = false;
		if (pluspom == true) {
			plus = true;
			//report_info("postavili opet plus", FactorExpr);
		}
		if (minuspom == true) {
			minus = true;
			//report_info("postavili opet minus", FactorExpr);
		}
		if (mulpom == true) {
			mul = true;
//			report_info("postavili opet mul", FactorExpr);
		}
		if (divpom == true) {
			div = true;
//			report_info("postavili opet div", FactorExpr);
		}
		minuspom = false;
		pluspom = false;
		mulpom = false;
		divpom = false;

	}

	public void visit(LarenPom LarenPom) {
		if (plus == true) {
//			report_info("bio plus pre", LarenPom);
			pluspom = true;
		}
		if (minus == true) {
//			report_info("bio minus pre", LarenPom);
			minuspom = true;
		}
		if (mul == true) {
//			report_info("bio plus pre", LarenPom);
			mulpom = true;
		}
		if (div == true) {
//			report_info("bio minus pre", LarenPom);
			divpom = true;
		}
		plus = false;
		minus = false;
		mul = false;
		div = false;
	}

	public void visit(ExprNov ExprNov) {
		ternarni = 1;
		// report_info("usp", ExprNov);
		Expr1 e = ExprNov.getExpr1();
		if (e.obj != null) {
			// report_info("mozda adresa" + e.obj.getAdr(), e);
		}
		Code.put2(drugipatch, Code.pc - drugipatch + 1);
	}

	private static final int TWO_JMP_OFFSET_SIZE = 6;
	private static final int DUMMY = 4;
	public int prvipatch = 0;
	public int drugipatch = 0;

	public void visit(UpitnikPom UpitnikPom) {

		Code.loadConst(0);
		Code.put(Code.jcc + Code.eq); // 16

		// report_info("pre" + Code.pc, UpitnikPom);

		prvipatch = Code.pc;
		Code.put2(DUMMY);
		// report_info("posle" + Code.pc, UpitnikPom);

//		ovde stavljam jump i promenjivu za 3 expr1
	}

	public void visit(DvotackaPom DvotackaPom) {

		// report_info("zvao iz upitnika kod dvotacke" + " pc " + Code.pc, DvotackaPom);

		Code.put2(prvipatch, Code.pc - prvipatch + 4);
//		Code.pc - addressToPatch + 1
		Code.put(Code.jmp);
		drugipatch = Code.pc;
		Code.put2(DUMMY);
//		Code.loadConst(25);

	}

	public void visit(ExprCond ExprCond) {
//		ExprCond.obj = new Obj(Obj.Var, "d", Tab.intType);
		// report_info("bio ovde", ExprCond);

	}

	int gde = 0;
	int pcc = 0;
	private boolean haveMorePrints = false;

	public void visit(NormalExpr NormalExpr) {
		gde++;
		if (gde > 3) {
			gde = 0;
		}
		if (gde == 2) {
			pcc = Code.pc;
		}
		NormalExpr.obj = new Obj(Obj.Var, "d", Tab.intType);
		NormalExpr.obj.setAdr(Code.pc);

	}

	public void visit(HaveNumConst HaveNumConst) {
		haveMorePrints = true;
	}

	private void defineOrdCall() {
		Obj ordObj = Tab.find("ord");
		ordObj.setAdr(Code.pc);

		Code.put(Code.return_);
	}

	private void defineChrCall() {
		Obj chrObj = Tab.find("chr");
		chrObj.setAdr(Code.pc);

		Code.put(Code.return_);
	}

	private void defineLenCall() {
		Obj lenObj = Tab.find("len");
		lenObj.setAdr(Code.pc);

		Code.put(Code.arraylength);
		Code.put(Code.return_);
	}
}
