package Translate;
import syntaxtree.And;
import syntaxtree.ArrayAssign;
import syntaxtree.ArrayLength;
import syntaxtree.ArrayLookup;
import syntaxtree.Assign;
import syntaxtree.Block;
import syntaxtree.BooleanType;
import syntaxtree.Call;
import syntaxtree.ClassDeclExtends;
import syntaxtree.ClassDeclSimple;
import syntaxtree.False;
import syntaxtree.Formal;
import syntaxtree.Identifier;
import syntaxtree.IdentifierExp;
import syntaxtree.IdentifierType;
import syntaxtree.If;
import syntaxtree.IntArrayType;
import syntaxtree.IntegerLiteral;
import syntaxtree.IntegerType;
import syntaxtree.LessThan;
import syntaxtree.MainClass;
import syntaxtree.MethodDecl;
import syntaxtree.Minus;
import syntaxtree.NewArray;
import syntaxtree.NewObject;
import syntaxtree.Not;
import syntaxtree.Plus;
import syntaxtree.Print;
import syntaxtree.Program;
import syntaxtree.This;
import syntaxtree.Times;
import syntaxtree.True;
import syntaxtree.VarDecl;
import syntaxtree.While;
import visitor.ExpVisitor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

import Frame.Access;
import Temp.Label;
import Temp.Temp;
import Tree.ExpList;

public class Translate implements ExpVisitor
{

  private Frag   frags     = null;
  private Frag   frags_tail = null;
  private Frame.Frame              currFrame = null;
  private Tree.Exp                 objPtr    = null;
  private int                      offset;
  private HashMap<String, Integer> fieldVars = null;
  private HashMap<String, Access>  vars      = null;

  public Translate(Program p, Frame.Frame f)
  {
    currFrame = f;
    p.accept(this);
  }

  public void procEntryExit(Tree.Stm body)
  {
    ProcFrag newfrag = new ProcFrag(body, currFrame);
    if (frags == null)
      frags = newfrag;
    else
      frags_tail.next = newfrag;
    frags_tail = newfrag;
    
  }

  public Frag getResults()
  {
    return frags;
  }

  public void printResults()
  {
    Tree.Print p = new Tree.Print(System.out);
    Frag f = frags;
    while (f != null)
      {
        System.out.println();
        System.out.println("Function: " + ((ProcFrag) f).frame.name.toString());
        p.prStm(((ProcFrag) f).body);
        f = f.next;
      }
  }

  public Exp visit(Program n)
  {
    n.m.accept(this);
    for (int i = 0; i < n.cl.size(); i++)
      n.cl.elementAt(i).accept(this);
    return null;
  }

  public Exp visit(MainClass n)
  {  
	Frame.Frame newFrame = currFrame.newFrame("main", 1);
    Frame.Frame oldFrame = currFrame;
    currFrame = newFrame;

    Tree.Stm s = (n.s.accept(this)).unNx();

    Tree.Exp retExp = new Tree.CONST(0);
    Tree.Stm body = new Tree.MOVE(new Tree.TEMP(currFrame.RV()), new Tree.ESEQ(s, retExp));

    procEntryExit(body);
    currFrame = oldFrame;

    return null;
  }

  public Exp visit(ClassDeclSimple n)
  {
    fieldVars = new HashMap<String, Integer>();
    offset = -1 * currFrame.wordSize(); 
    for (int i = 0; i < n.vl.size(); i++)
      n.vl.elementAt(i).accept(this);
    for (int i = 0; i < n.ml.size(); i++)
      n.ml.elementAt(i).accept(this);
    fieldVars = null;
    return null;
  }

  public Exp visit(ClassDeclExtends n)
  {
    return null;
  }

  public Exp visit(VarDecl n)
  {
    if (vars == null)
      fieldVars.put(n.i.toString(), new Integer(offset += currFrame.wordSize()));
    else
      vars.put(n.i.toString(), currFrame.allocLocal(false));

    return null;
  }

  public Exp visit(MethodDecl n)
  {
    Frame.Frame newFrame = currFrame.newFrame(n.i.toString(), n.fl.size() + 1);
    Frame.Frame oldFrame = currFrame;
    currFrame = newFrame;

    for (int i = 0; i < n.vl.size(); i++)
      n.vl.elementAt(i).accept(this);

    /* ADD CODE: move formals to fresh temps and add them to the HashMap vars */
    
    /* ADD CODE: set value of Tree.Exp objPtr
     Recall that objPtr is a pointer to the address in memory at which 
     instance variables are stored for the current class 
     (i.e., it is "this").
     In the MiniJava compiler, it is passed as an argument during all
     calls to MiniJava methods. */

    /* ADD CODE: visit each statement in method body, 
     creating new Tree.SEQ nodes as needed */
    Tree.Stm body = null; // FILL IN

    /* ADD CODE: get return expression and group with statements of body,
     then create Tree.MOVE to store return value */

    /* create new procedure fragment for method and add to list */
    procEntryExit(body);
    currFrame = oldFrame;
    vars = null;
    objPtr = null;

    return null;
  }

  public Exp visit(Formal n)
  {
    return null;
  }

  public Exp visit(IntArrayType n)
  {
    return null;
  }

  public Exp visit(BooleanType n)
  {
    return null;
  }

  public Exp visit(IntegerType n)
  {
	  return null;
  }

  public Exp visit(IdentifierType n)
  {
    return null;
  }

  public Exp visit(Block n)
  {
	  for ( int i = 0; i < n.sl.size(); i++ ) {
	        n.sl.elementAt(i).accept(this);
	  }
	  return null;
      //return new Nx(stm);

  }

  public Exp visit(If n)
  {
	  Label T = new Label();
      Label F = new Label();
      Label D = new Label();
      Exp exp =  n.e.accept(this);
      Exp stmT =  n.s1.accept(this);
      Exp stmF =  n.s2.accept(this);
      return new Nx(new Tree.SEQ
		     (new Tree.SEQ
		      (new Tree.SEQ
		       (new Tree.SEQ
			(new Tree.CJUMP(Tree.CJUMP.EQ, exp.unEx(),new Tree.CONST(1),T,F),
					new Tree.SEQ(new Tree.LABEL(T),stmT.unNx())),
					new Tree.JUMP(D)),
					new Tree.SEQ(new Tree.LABEL(F),stmF.unNx())),
					new Tree.LABEL(D)));

  }

  public Exp visit(While n)
  {
	  Label test = new Label();
      Label T = new Label();
      Label F = new Label();
      Exp exp = n.e.accept(this);
      Exp body = n.s.accept(this);
      
      return new Nx(new Tree.SEQ
		     (new Tree.SEQ
		      (new Tree.SEQ(new Tree.LABEL(test),
			   (new Tree.CJUMP(Tree.CJUMP.EQ, exp.unEx(), 
					   new Tree.CONST(1),T,F))),
		       (new Tree.SEQ( new Tree.LABEL(T),body.unNx()))),
		       new Tree.LABEL(F)));

  }

  public Exp visit(Print n)
  {
	  Tree.Exp e = (n.e.accept(this)).unEx(); 
      return new Nx(new Tree.MOVE(new Tree.TEMP(new Temp()),
    		  new Tree.CALL(new Tree.NAME(new Label("_printint")),new Tree.ExpList(e, null))));
  }

  public Exp visit(Assign n)
  {  
    Tree.Exp e = (n.e.accept(this)).unEx();
    return new Nx(new Tree.MOVE(getIdTree(n.i.toString()), e));
  }

  public Exp visit(ArrayAssign n)
  {
	Tree.Exp e1 = (n.e1.accept(this)).unEx();
	Tree.Exp e2 = (n.e2.accept(this)).unEx();
	Tree.Exp expId = (n.i.accept(this)).unEx();
	return new Nx(new Tree.MOVE(new Tree.BINOP(Tree.BINOP.PLUS,new Tree.MEM(expId), new Tree.BINOP(Tree.BINOP.MUL,e1,new Tree.CONST(4))), e2)); 
  }

  public Exp visit(And n)
  {
	  Temp t1 = new Temp();
      Label done = new Label();
      Label ok1 = new Label();
      Label ok2 = new Label();
      
      Tree.Exp left =  n.e1.accept(this).unEx();
      Tree.Exp right = n.e2.accept(this).unEx();
      return new Ex
	  (new Tree.ESEQ(new Tree.SEQ
		(new Tree.SEQ
		 (new Tree.SEQ
		  (new Tree.SEQ (new Tree.SEQ (new Tree.MOVE(new Tree.TEMP(t1),new Tree.CONST(0)),
				  new Tree.CJUMP(Tree.CJUMP.EQ, left, new Tree.CONST(1), ok1, done)),
				  new Tree.SEQ(new Tree.LABEL(ok1), 
						  new Tree.CJUMP(Tree.CJUMP.EQ, right, new Tree.CONST(1), ok2, done))), 
			    new Tree.SEQ(new Tree.LABEL(ok2),  new Tree.MOVE(new Tree.TEMP(t1),new Tree.CONST(1)))),
			    new Tree.JUMP(done)),
			    new Tree.LABEL(done)),
			    new Tree.TEMP(t1)));

  }

  public Exp visit(LessThan n)
  {
	  Exp expl= n.e1.accept(this);
      Exp expr= n.e2.accept(this);
      Label T = new Label();
      Label F = new Label();
      Temp t = new Temp();
      return new Ex
	  (new Tree.ESEQ(new Tree.SEQ
		(new Tree.SEQ
		 (new Tree.SEQ
		  (new Tree.MOVE(new Tree.TEMP(t),new Tree.CONST(0)),
				  new Tree.CJUMP(Tree.CJUMP.LT,expl.unEx(),expr.unEx(),T,F)),
				  new Tree.SEQ(new Tree.LABEL(T), new Tree.MOVE(new Tree.TEMP(t),new Tree.CONST(1)))),
				  new Tree.LABEL(F)),
				  new Tree.TEMP(t)))  ;
  }

  public Exp visit(Plus n)
  {
	Tree.Exp exp1 = (n.e1.accept(this)).unEx();
	Tree.Exp exp2 = (n.e2.accept(this)).unEx();
	return new Ex(new Tree.BINOP(Tree.BINOP.PLUS, exp1, exp2));
  }

  public Exp visit(Minus n)
  {
	Tree.Exp exp1 = (n.e1.accept(this)).unEx();
	Tree.Exp exp2 = (n.e2.accept(this)).unEx();
	return new Ex(new Tree.BINOP(Tree.BINOP.MINUS, exp1, exp2));
  }

  public Exp visit(Times n)
  {
	Tree.Exp exp1 = (n.e1.accept(this)).unEx();
	Tree.Exp exp2 = (n.e2.accept(this)).unEx();
	return new Ex(new Tree.BINOP(Tree.BINOP.MUL, exp1, exp2));
  }

  public Exp visit(ArrayLookup n)
  {
	  Temp t_index = new Temp();
      Temp t_size = new Temp();
      Tree.Exp e1 = n.e1.accept(this).unEx();
      Tree.Exp e2 = n.e2.accept(this).unEx();

      Label F = new Label();
      Label T = new Label();
      
      Tree.ExpList args1 = new ExpList(e2, null);      
      
      Tree.Stm s1 = 
    	  new Tree.SEQ
	  (new Tree.SEQ
	   (new Tree.SEQ
	    (new Tree.SEQ
	     (new Tree.SEQ
	      (new Tree.MOVE(new Tree.TEMP(t_index),new Tree.BINOP(Tree.BINOP.MUL,e2,new Tree.CONST(4))),
	    		  new Tree.MOVE(new Tree.TEMP(t_size),new Tree.MEM(e1))),
	    		  new Tree.CJUMP(Tree.CJUMP.GE,new Tree.TEMP(t_index),new Tree.TEMP(t_size),T,F)),
	    		  new Tree.LABEL(T)),
	    		  new Tree.MOVE(new Tree.TEMP(new Temp()),
	    				  new Tree.CALL(new Tree.NAME(new Label("_error")),args1))),
	    				  new Tree.LABEL(F));
	  
      Temp t = new Temp();
      Tree.Stm s2 = new Tree.SEQ
	  (s1,new Tree.MOVE(new Tree.TEMP(t),new Tree.MEM
		   (new Tree.BINOP(Tree.BINOP.PLUS,e1,new Tree.BINOP
			  (Tree.BINOP.PLUS,
					  new Tree.BINOP(Tree.BINOP.MUL,e2,new Tree.CONST(4))
			   ,new Tree.CONST(4))))));
      return new Ex(new Tree.ESEQ(s2,new Tree.TEMP(t)));

  }

  public Exp visit(ArrayLength n)
  {
      n.e.accept(this);
      return null;

  }

  public Exp visit(Call n)
  {
    /* ADD CODE -- don't return null */
    return null;
  }

  public Exp visit(IntegerLiteral n)
  {
	return new Ex(new Tree.CONST(n.i));
  }

  public Exp visit(True n)
  {
	 return new Ex(new Tree.CONST(1));
  }

  public Exp visit(False n)
  {
	  return new Ex(new Tree.CONST(0));
  }

  public Exp visit(IdentifierExp n)
  {
    return new Ex(getIdTree(n.s));
  }

  public Exp visit(This n)
  {
    return new Ex(objPtr);
  }

  public Exp visit(NewArray n)
  {
    /* ADD CODE
     (Note: use currFrame.externalCall("alloc", new Tree.ExpList(...))) 
     -- don't return null */
    return null;
  }

  public Exp visit(NewObject n)
  {
    /* ADD CODE
     (Note: you will need to get the number of field variables from your
     symbol table)  -- don't return null */
    return null;
  }

  public Exp visit(Not n)
  {
    return new Ex
	   (new Tree.BINOP(Tree.BINOP.MINUS, new Tree.CONST(1), 
		  (n.e.accept(this)).unEx()));
  }

  public Exp visit(Identifier n)
  {
    return null;
  }

  private Tree.Exp getIdTree(String id)
  {
    Frame.Access a = vars.get(id);
    if (a == null)
      {
        int offset = fieldVars.get(id).intValue();
        return new Tree.MEM(new Tree.BINOP(Tree.BINOP.PLUS, objPtr, new Tree.CONST(offset)));
      }

    return a.exp(new Tree.TEMP(currFrame.FP()));
  }
}
