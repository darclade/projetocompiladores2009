package syntaxtree;
import visitor.Visitor;
import visitor.TypeVisitor;
import visitor.ExpVisitor;

public abstract class ClassDecl {
  public abstract void accept(Visitor v);
  public abstract Type accept(TypeVisitor v);
  public abstract Translate.Exp accept(ExpVisitor v);
}
