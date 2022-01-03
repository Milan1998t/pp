package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticPass extends VisitorAdaptor {

	Logger log = Logger.getLogger(getClass());
	public int niz = 0;
	public Struct typee = Tab.noType;
	static Struct boolType = new Struct(Struct.Bool);
	public Struct methodType = Tab.noType;
	static Struct classType = new Struct(Struct.Class);
	Obj currentMethod = null;
	Obj currentClass = null;
	public Struct des = null;
	public int exprType = 9;
	public int constType = 9;
	public boolean foundMain = false;
	public boolean foundMainn = false;
	public boolean noFormPars = true;
	public boolean errorFound = false;
	public boolean factorNewBrack = false;
	public boolean foundReturn = false;
	public boolean doPetlja = false;
	public boolean switchPetlja = false;
	public int nVars = 0;

	public void report_error(String message, SyntaxNode info) {
		errorFound = true;
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

	public String gtKind(int i) {
		if (i == 0) {
			return "Con";
		}
		if (i == 1) {
			return "Var";
		}
		if (i == 2) {
			return "Type";
		}
		if (i == 3) {
			return "Meth";
		}
		if (i == 4) {
			return "Fld";
		}
		if (i == 5) {
			return "Elem";
		}
		if (i == 6) {
			return "Prog";
		}
		return "";
	}

	public String gtType(int i) {
		if (i == 0) {
			return "None";
		}
		if (i == 1) {
			return "Int";
		}
		if (i == 2) {
			return "Char";
		}
		if (i == 3) {
			return "Array";
		}
		if (i == 4) {
			return "Class";
		}
		if (i == 5) {
			return "Bool";
		}
		return "";
	}

	public String pravi(Obj o) {
		if (o == null)
			return "";
		return gtKind(o.getKind()) + " " + o.getName() + ":" + gtType(o.getType().getKind()) + " " + o.getAdr() + ","
				+ o.getLevel();
	}

	public void visit(ProgName progName) {
		report_info("Ime programa " + progName.getProgName(), null);
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
	}

	public void visit(Program Program) {
		Tab.chainLocalSymbols(Program.getProgName().obj);
		Tab.closeScope();
		report_info("===================================", null);
		if (foundMain) {
			report_info("Postoji void main", null);
		} else {
			report_error("Ne postoji void main", null);
		}
		if (errorFound == true) {
			report_error("Semantika ima gresku", null);
		}
	}

	public void visit(Type type) {
		typee = type.struct;
		// nzm da li treba ovo gore ovako
		Obj typeNode = Tab.find(type.getTypeName());
		if (typeNode == Tab.noObj) {
			if (type.getTypeName().equals("bool")) {
				typee = boolType;
				return;
			}
			report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
			type.struct = Tab.noType;
		} else {
			if (Obj.Type == typeNode.getKind()) {
				type.struct = typeNode.getType();
				typee = type.struct;
				// report_error("Tip je" + type.struct.getKind() , type);
			} else {
				if (typeNode.getKind() == 4) {
					typee = type.struct;
					return;
				}
				report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
				type.struct = Tab.noType;
			}
		}
	}

	public void visit(VarDeclIskaz VarDeclIskaz) {
		if (typee != Tab.noType) {
			// report_info("typee nije noType "+ typee.getKind()+ " je type", null);
		}
		Obj o = Tab.currentScope().findSymbol(VarDeclIskaz.getVarName());

		if (o == null) {
			// report_info("tip u iskazu " + typee.getKind(), null);

			if (niz == 1) {
				Struct pom = new Struct(3);
				pom.setElementType(typee);

				Obj c = Tab.insert(Obj.Var, VarDeclIskaz.getVarName(), pom);
				report_info("Deklarisana promenjiva " + pravi(c), VarDeclIskaz);
				nVars += 1;
			} else {
				Obj c = Tab.insert(Obj.Var, VarDeclIskaz.getVarName(), typee);
				report_info("Deklarisana promenjiva " + pravi(c), VarDeclIskaz);
				nVars += 1;
			}
		} else {
			if (o == Tab.chrObj || o == Tab.lenObj || o == Tab.ordObj) {
				report_error(VarDeclIskaz.getVarName() + " je vec deklarisana", VarDeclIskaz);
			}

			report_error("Deklarisana promenjiva " + VarDeclIskaz.getVarName() + " je vec deklarisana", VarDeclIskaz);
			// report_error( o.getName(), null);
		}
		niz = 0;
		// report_info(o.getName(), null);
	}

	public void visit(ConstDeclTerm ConstDeclTerm) {

		if (typee != Tab.noType) {
			// report_info("typeopt ispisuje " + typee.getKind(), null);
		}
		Obj o = Tab.currentScope().findSymbol(ConstDeclTerm.getConstName());

		if (o == null) {
			if (typee.getKind() == constType) {
				Obj c = Tab.insert(Obj.Con, ConstDeclTerm.getConstName(), typee);
				report_info("deklarisana konstanta " + pravi(c), ConstDeclTerm);
			} else {
				// report_error("nisu kompatibilni tipovi" + typee.getKind() + constType, null);
				report_error("nisu kompatibilni tipovi" + typee.getKind() + constType, null);
			}

		} else {
			report_error("Deklarisana konstanta " + ConstDeclTerm.getConstName() + " je vec deklarisana",
					ConstDeclTerm);
		}
	}

	public void visit(AssingOpNumber AssingOpNumber) {
		constType = 1;
	}

	public void visit(AssingOpChar AssingOpChar) {
		constType = 2;
	}

	public void visit(AssingOpBool AssingOpBool) {
		constType = 5;
	}

	public void visit(TypeOpt TypeOpt) {
		methodType = TypeOpt.getType().struct;
		// report_info("typeopt ispisuje " + TypeOpt.getType().getTypeName(), null);
		// nzm da li treba ovo gore ovako
		Obj typeNode = Tab.find(TypeOpt.getType().getTypeName());
		if (typeNode == Tab.noObj) {
			report_error("Nije pronadjen tip " + TypeOpt.getType().getTypeName() + " u tabeli simbola! ", null);
			TypeOpt.getType().struct = Tab.noType;
		} else {
			if (Obj.Type == typeNode.getKind()) {
				TypeOpt.getType().struct = typeNode.getType();
				methodType = TypeOpt.getType().struct;
			} else {
				report_error("Greska: Ime " + TypeOpt.getType().getTypeName() + " ne predstavlja tip!", TypeOpt);
				TypeOpt.getType().struct = Tab.noType;
			}
		}
	}

	public void visit(VoidOpt VoidOpt) {
		// treba da se vidi sta je kada je void
		methodType = Tab.noType;
		// report_info("typeopt ispisuje void", null);
	}

	public void visit(MethodDeclName MethodDeclName) {

		report_info("===================================", null);
		report_info("Obradjuje se f-ja " + MethodDeclName.getMethodName(), MethodDeclName);
		if (MethodDeclName.getMethodName().equals("main")) {
			if (methodType == Tab.noType) {
				foundMainn = true;
			}
		}
		currentMethod = Tab.insert(Obj.Meth, MethodDeclName.getMethodName(), methodType);
		Tab.openScope();
		// MethodDeclName.obj= currentMethod; nzm sta ce mi ovo

	}

	public void visit(MethodDecl MethodDecl) {
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		currentMethod = null;
		report_info("Zatvara se f-ja " + MethodDecl.getMethodDeclName().getMethodName(), null);
		if (foundReturn || methodType.getKind() == 0) {
			report_info("f-ja ima return ", null);
		} else {
			report_error("f-ja nema return ", null);
		}
		foundReturn = false;
		// report_info("===================================",null);
	}

	public void visit(DsInc DsInc) {
		Obj o = Tab.currentScope.findSymbol(DsInc.getDesignator().getVarName());
		if (o != null && o.getType().getKind() == 1) {
			report_info("Designator " + DsInc.getDesignator().getVarName() + " odradio ++", DsInc);
		} else {
			if (o == null) {
				o = Tab.find(DsInc.getDesignator().getVarName());
				if (o != null && o.getType().getKind() == 1) {
					report_info("Designator " + DsInc.getDesignator().getVarName() + " odradio ++", DsInc);
				} else {
					if (o.getType().getKind() == 3 && o.getType().getElemType().getKind() == 1) {
						report_info("Designator " + DsInc.getDesignator().getVarName() + " odradio ++", DsInc);

					} else {
						report_error("Designator " + DsInc.getDesignator().getVarName() + " nije integer", DsInc);
					}
				}
//				report_info("Designator " + DsInc.getDesignator().getVarName() + " nije definisan", DsInc);
			} else {
				if (o.getType().getKind() == 3 && o.getType().getElemType().getKind() == 1) {
					report_info("Designator " + DsInc.getDesignator().getVarName() + " odradio ++", DsInc);

				} else {
					report_error("Designator " + DsInc.getDesignator().getVarName() + " nije integer", DsInc);
				}
			}
		}

	}

	public void visit(DsDec DsDec) {
		Obj o = Tab.currentScope.findSymbol(DsDec.getDesignator().getVarName());
		if (o != null && o.getKind() == 1 && o.getType().getKind() == 1) {
			report_info("Designator " + DsDec.getDesignator().getVarName() + " odradio --", DsDec);
		} else {
			if (o == null) {
				report_info("Designator " + DsDec.getDesignator().getVarName() + " nije definisan", DsDec);
			} else {
				if (o.getKind() != 1) {
					report_info("Designator " + DsDec.getDesignator().getVarName() + " nije variabla", DsDec);
				}
				if (o.getType().getKind() != 1) {
					report_info("Designator " + DsDec.getDesignator().getVarName() + " nije integer", DsDec);
				}
			}
		}

	}

	public void visit(DsEqual DsEqual) {

		Obj o = Tab.currentScope.findSymbol(DsEqual.getDesignator().getVarName());
		if (o != null) {
			report_info("" + pravi(o), DsEqual);
		} else {
			o = Tab.find(DsEqual.getDesignator().getVarName());
			if (o != null) {
				report_info("" + pravi(o), DsEqual);
			}
		}

		o = Tab.currentScope.findSymbol(DsEqual.getDesignator().getVarName());
		if (o == null) {
			o = Tab.find(DsEqual.getDesignator().getVarName());
			if (o == Tab.noObj) {
				// report_error("Nije definisan " + DsEqual.getDesignator().getVarName(),
				// DsEqual);
				return;
			}
		}

		if (o == null || o == Tab.noObj) {
			// report_error("Nije definisan " + DsEqual.getDesignator().getVarName(),
			// DsEqual);
			return;
		}

		// report_error("Nije definisan " + o.getType().getKind(), DsEqual);
		if (o.getType().getKind() == 4) {
			if (exprType == 7) {
				report_info("Klasi je dodeljena null vrednost", DsEqual);
				return;
			} else {
				report_error("Nije moguce dodeliti trazenu vrednost vrednost", DsEqual);
				return;
			}
		}

		// report_error("Nije definisan " + DsEqual.getDesignator().getVarName(),
		// DsEqual);
		// report_info(""+DsEqual.getDesignator().getVarName()+exprType, DsEqual);
		if (o.getType().getKind() == 3) {
			if (exprType == 7) {
				report_info("Nizu je dodeljena null vrednost", DsEqual);
				return;
			}
			if (exprType == 1 && o.getType().getElemType().getKind() != 5 && o.getType().getElemType().getKind() != 2) {
				report_info("Nizu je dodeljena int vrednost", DsEqual);
				return;
			}
			// report_info(""+o.getType().getElemType().getKind()+exprType, DsEqual);
			if (o.getType().getElemType().getKind() != exprType) {
				if (exprType == 8) {
					report_error("Nije pronadjena desna strana", DsEqual);
				}
				// report_error("Tipovi nisu
				// kompatibilni"+o.getType().getKind()+exprType,DsEqual);
				report_error("Tipovi nisu kompatibilni", DsEqual);
			} else {
				report_info("Designator " + DsEqual.getDesignator().getVarName() + " je odradio nesto", DsEqual);
			}

			// report_info("za niz tip " + o.getType().getElemType().getKind(), null);
		} else {

			if (o.getType().getKind() != exprType) {
				if (exprType == 8) {
					report_error("Nije pronadjena desna strana", DsEqual);
				}
				report_error("Tipovi nisu kompatibilni " + DsEqual.getDesignator().getVarName() + exprType, DsEqual);
			} else {
				report_info("Designator " + DsEqual.getDesignator().getVarName() + " je odradio nesto", DsEqual);
			}
		}

		exprType = 9;
	}

	public void visit(NormalExpr NormalExpr) {
		NormalExpr.getTerm().getFactor();
	}

	public void visit(FactorDesignator FactorDesignator) {
		Obj o = Tab.currentScope.findSymbol(FactorDesignator.getDesignator().getVarName());
		if (o != null) {
			report_info("" + pravi(o), FactorDesignator);
		} else {
			o = Tab.find(FactorDesignator.getDesignator().getVarName());
			if (o != null) {
				report_info("" + pravi(o), FactorDesignator);
			}
		}
		int pronasao = 0;
		if (FactorDesignator.getDesignator().getVarName().equals("null")) {
			exprType = 7;
			return;
		}
		o = Tab.currentScope.findSymbol(FactorDesignator.getDesignator().getVarName());

		if (o != null && o.getType().getKind() == 3) {
			// report_info("ovde" + FactorDesignator.getDesignator().getVarName() +
			// exprType, FactorDesignator);
			if (exprType == 1 && o.getType().getKind() == 3 && o.getType().getElemType().getKind() == 2) {
				exprType = 2;
			}

			if (o.getType().getElemType().getKind() != exprType) {
				exprType = 8;
			} else {
				return;
			}
		}
		if (o == null) {
			o = Tab.find(FactorDesignator.getDesignator().getVarName());
			if (o != null && o.getType().getKind() == 3) {
				// report_info("ovde" + FactorDesignator.getDesignator().getVarName() +
				// exprType, FactorDesignator);
				if (exprType == 1 && o.getType().getKind() == 3 && o.getType().getElemType().getKind() == 2) {
					exprType = 2;
				}

				if (o.getType().getElemType().getKind() != exprType) {
					exprType = 8;
				} else {
					return;
				}
			}
			if (o == Tab.noObj) {
				report_error("Nije definisan" + FactorDesignator.getDesignator().getVarName(), null);
				return;
			} else {
				if (o.getLevel() == 0) {
					pronasao = 1;
				}
			}
		}
		if (pronasao == 0) {
			o = Tab.currentScope.findSymbol(FactorDesignator.getDesignator().getVarName());
		}
		if (o != null && (o.getKind() == 0 || o.getKind() == 1)) {
			if (exprType == 9) {
				// report_error("Tipovi su kompatibilni", null);
				exprType = o.getType().getKind();
			} else {
				if (exprType != o.getType().getKind()) {
					// report_error("Tipovi nisu kompatibilni", null);
					exprType = o.getType().getKind();
				}
			}
		}
		if (o == null) {
			o = Tab.find(FactorDesignator.getDesignator().getVarName());
			if (o != null && o.getKind() == 0 && o.getType().getKind() == 1) {
				if (exprType == 9) {
					// report_error("Tipovi su kompatibilni", null);
					exprType = o.getType().getKind();
				} else {
					if (exprType != o.getType().getKind()) {
						// report_error("Tipovi nisu kompatibilni", null);
						exprType = o.getType().getKind();
					}
				}
			} else {
				if (o != null) {
					exprType = 9;
				} else {
					exprType = 8;
					report_error("Ne postoji designator", null);
				}
			}
		}
	}

	public void visit(FactorNumConst FactorNumConst) {

		if (exprType == 9) {
			exprType = 1;
		} else {
			if (exprType != 1) {
				// report_error("Tipovi nisu kompatibilni", null);
				exprType = 1;
			}
		}

	}

	public void visit(FactorCharConst FactorCharConst) {

		if (exprType == 9) {
			exprType = 2;
			// report_error("char uso", null);
		} else {
			if (exprType != 2) {
				// report_error("Tipovi nisu kompatibilni", null);
				exprType = 2;
				// report_error("char uso1", null);
			}
		}
	}

	public void visit(FactorNew FactorNew) {
		if (FactorNew.getType().struct.getKind() == 1) {
			exprType = 1;
			return;
		} else {
			if (FactorNew.getType().struct.getKind() == 2) {
				exprType = 2;
				return;
			}
			exprType = 8;
		}
	}

	public void visit(FactorBoolConsts FactorBoolConst) {
		// uopste ne pozove ovu fju
		// report_error("uso", FactorBoolConst);

		if (exprType == 9) {
			exprType = 5;
		} else {
			if (exprType != 5) {
				// report_error("Tipovi nisu kompatibilni", null);
				exprType = 5;
			}
		}
	}

	public void visit(FactorNewBrack FactorNewBrack) {
		// report_info("ko"+FactorNewBrack.getType().getTypeName(), FactorNewBrack);
		factorNewBrack = true;
		if (exprType != 1) {
			exprType = 9;
		} else {
			if (FactorNewBrack.getType().getTypeName().equals("bool")) {
				exprType = 5;
				return;
			}
			if (FactorNewBrack.getType().struct.getKind() == 1) {
				exprType = 1;
				return;
			} else {
				if (FactorNewBrack.getType().struct.getKind() == 2 || FactorNewBrack.getType().struct.getKind() == 5) {
					exprType = 2;
					return;
				}
				exprType = 8;
			}
		}
	}

	// ovde treba da se vidi ako je exp razlicit da nisu dobri tipovi

	public void visit(Designator Designator) {

		Obj o = Tab.currentScope.findSymbol(Designator.getVarName());
		if (o == null) {
			// report_info("Designator "+ Designator.getVarName() +" nije definisan", null);
		}
	}

	public void visit(BraceOpt BraceOpt) {
		niz = 1;
	}

	public void visit(NoFormParsList NoFormParsList) {
		// report_info("uso", NoFormParsList);
		if (foundMainn) {
			foundMain = true;
		}
		noFormPars = true;
	}

	public void visit(FormParsLists FormParsLists) {
		// report_error("uso1", FormParsLists);
		noFormPars = false;
	}

	public void visit(RetrunVoid RetrunVoid) {
		if (currentMethod == null) {
			report_error("return ne sme ovde da se nadje", null);
			return;
		}
		if (methodType.getKind() == 0) {
			report_info("dobra povratna vrednost fje void", RetrunVoid);
			foundReturn = true;
		} else {
			report_error("F-ja mora da ima povratnu vrednost", null);
		}

	}

	public void visit(HaveReturn HaveReturn) {
		if (currentMethod == null) {
			report_error("return ne sme ovde da se nadje", null);
			return;
		}
		if (methodType.getKind() == 0) {
			report_error("greska f-ja nema povratni tip", null);
		} else {
			Obj o = Tab.currentScope.findSymbol(HaveReturn.getIdentName());
			if (o != null) {
				if (o.getType().getKind() == methodType.getKind()) {
					report_info("F-ja ima dobru povratnu vrednost", HaveReturn);
					foundReturn = true;
				} else {
					report_error("greska f-ja nema dobru povratnu vrednost", null);
				}
			} else {
				report_error("greska povratna vrednost ne postoji", null);
			}

		}
	}

	public void visit(ClassName ClassName) {
		report_info("===================================", null);

		currentClass = Tab.insert(Obj.Type, ClassName.getClassName(), classType);
		report_info("Obradjuje se klasa" + ClassName.getClassName(), ClassName);

		Tab.openScope();
	}

	public void visit(ClassDecl ClassDecl) {
		Tab.chainLocalSymbols(currentClass);
		Tab.closeScope();
		currentClass = null;
		report_info("Zatvara se klasa " + ClassDecl.getClassName().getClassName(), null);

	}

	public void visit(DoStatement DoStatement) {
		// report_error("uso", DoStatement);
		doPetlja = false;
		if (exprType != 5) {
			report_error("condition nije tipa bool", DoStatement);
		}
	}

	public void visit(IfStatement IfStatement) {

	}

	public void visit(DoIdent DoIdent) {
		doPetlja = true;
	}

	public void visit(BreakStatement BreakStatement) {
		if (doPetlja == true || switchPetlja == true) {
			// report_info("break moze da se nadje na ovom mestu", BreakStatement);
		} else {
			report_error("break ne moze da se nadje na ovom mestu", BreakStatement);
		}
	}

	public void visit(SwitchIdent SwitchIdent) {
		switchPetlja = true;
	}

	List<Integer> caselist = new ArrayList<Integer>();

	public void visit(SwitchStatement SwitchStatement) {
		switchPetlja = false;
		caselist = null;
	}

	public void visit(ContinueStatement ContinueStatement) {
		if (doPetlja == true) {
			// report_info("continue moze da se nadje na ovom mestu", ContinueStatement);
		} else {
			report_error("continue ne moze da se nadje na ovom mestu", ContinueStatement);
		}
	}

	public void visit(IfConds IfConds) {
		if (exprType != 5) {
			report_error("if condition nije tipa bool", IfConds);
		}
	}

	public void visit(PomocniZa PomocniZa) {
		if (exprType != 1) {
			report_error("switch condition nije tipa int", PomocniZa);
		}
	}

	public void visit(CaseTerm CaseTerm) {
		if (caselist.contains(CaseTerm.getBroj())) {
			report_error("case sa tim brojem postoji", CaseTerm);
		} else {
			caselist.add(CaseTerm.getBroj());
		}

	}

	public void visit(CondFact CondFact) {
		if (exprType == 8 || exprType == 9) {
			report_error("tipovi nisu dobri condfact", CondFact);
		}
	}

	public void visit(Errore Errore) {
		report_info("greska", Errore);
	}

}
