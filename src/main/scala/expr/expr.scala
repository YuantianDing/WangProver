
package prover.expr


import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.math.max

final case class ExprError(
    private val message: String = "", 
    private val cause: Throwable = None.orNull
) extends Exception(message, cause) 

// -------------------------------------------------------------------------------------
// Expr
// Usage:
//      p -> q : "p -> q"
//      ~p : "¬p"
//      \(0x10) : "[10]" (replacer)
// for multiple ¬ operation, you can't simply use ~~p due to scala limitation
// you can use:
//      \ ~~p : "¬¬p"
//      !p  : "¬¬p"
//      -p  : "¬¬¬p"
//      +p  : "¬¬¬¬p"
// -------------------------------------------------------------------------------------


abstract class Expr {

    // stats
    val depth = 0            // depth
    val not_depth = 0        // depth that only take not operation in consideration
    val replacer_count = 0   // how many replacers expr in this Expr 
    val in_not_depth = 0     // depth that inside not operation
    val in_not_replacer = 0  // how many replacer inside not operation

    // display
    def bracketed() : Boolean
    override def toString() : String
    def getString = if (bracketed()) toString() else s"(${toString()})"

    // expression
    def -> (that : Expr) : Expr = new Implication(this, that)
    def | (that : Expr) = ~this -> that
    def & (that : Expr) = ~(this -> ~that)
    def <-> (that : Expr) = (this -> that) & (that -> this)
    def unary_~() : Expr = new Not(this)
    def unary_!() : Expr = ~(~this)
    def unary_-() : Expr = ~(~(~this))
    def unary_+() : Expr = ~(~(~(~this)))

    // utils
    def ===(that : Expr) : Boolean
    override def equals(x : Any): Boolean = 
        if(x.isInstanceOf[Expr]) this === x.asInstanceOf[Expr]
        else false

    override def hashCode(): Int = toString().hashCode()
    def search(i : Int, env : Map[Int, Expr]) : Boolean 

    // functions
    def eval() = real_eval()
    def real_eval() : Int

    def replace(env : Map[Int, Expr]) : Expr
    def matching(parttern : Expr, env : Map[Int, Expr]) : Boolean
}

object \ {
    def ~~(e : Expr) = ~(~e)
    def ~~~(e : Expr) = ~(~(~e))
    def ~~~~(e : Expr) = ~(~(~(~e)))
    def apply(i : Int) = new Replacer(i)
}

class Implication(val pre : Expr, val post : Expr) extends Expr {
    // display
    override def bracketed = false
    override def toString= pre.getString ++ " -> " ++ post.getString

    // stats
    override val replacer_count = pre.replacer_count + post.replacer_count
    override val not_depth = max(pre.not_depth, post.not_depth)
    override val depth = max(pre.depth, post.depth) + 1
    override val in_not_depth = max(pre.in_not_depth, post.in_not_depth)
    override val in_not_replacer = pre.in_not_replacer + post.in_not_replacer

    def ===(that : Expr) = if(that.isInstanceOf[Implication]) {
        val that0 = that.asInstanceOf[Implication]
        that0.pre === this.pre && that0.post === this.post
    } else false
    override def search(i : Int, env : Map[Int, Expr]) = pre.search(i,env) || post.search(i, env) 

    override def real_eval() = (~pre.eval() | post.eval()) & Eval.mask
    override def replace(env : Map[Int, Expr]) = {
        val (pre0, post0) = (pre.replace(env), post.replace(env))
        if(pre0 == pre && post0 == post)
            this
        else pre0 -> post0
    }
    override def matching(parttern : Expr, env : Map[Int, Expr]) = parttern match {
        case i : Implication => pre.matching(i.pre,env) && post.matching(i.post,env)
        case s : Replacer => parttern.matching(this, env)
        case _ => false
    }

}

class Not(val src : Expr) extends Expr {
    // display
    override def bracketed = true
    override def toString = "¬" ++ src.getString

    // stats
    override val replacer_count = src.replacer_count
    override val not_depth = src.not_depth + 1
    override val depth = src.depth + 1
    override val in_not_replacer = replacer_count
    override val in_not_depth = src match {
        case n : Not => n.in_not_depth
        case _ => depth
    }
    
    def ===(that : Expr) = if(that.isInstanceOf[Not]) {
        val that0 = that.asInstanceOf[Not]
        that0.src === this.src
    } else false

    override def real_eval() = ~ src.eval() & Eval.mask
    override def search(i : Int, env : Map[Int, Expr]) = src.search(i, env)

    override def replace(env : Map[Int, Expr]) = {
        val src0 = src.replace(env)
        if(src0 == src)
            this
        else ~src0
    }
    
    override def matching(parttern : Expr, env : Map[Int, Expr]) = parttern match {
        case n : Not => src.matching(n.src,env)
        case s : Replacer => parttern.matching(this, env)
        case _ => false
    }
}


