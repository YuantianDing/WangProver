package prover.expr

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.math.max

object Atom {
    // global control
    var symbols = new ArrayBuffer[String]()
    def apply(s:String) = new Atom(s)
}

class Atom(symbol : String) extends Expr {
    // global control
    val id = Atom.symbols.length
    Atom.symbols += symbol

    // display
    override def toString() = Atom.symbols(id)
    override def bracketed = true

    def ===(that : Expr) =
        if(that.isInstanceOf[Atom])
            that.asInstanceOf[Atom].id == id
        else false

    override def search(i : Int, env : Map[Int, Expr]) : Boolean = false

    override def real_eval() = {
        if(Eval.count == 0)
            ExprError("You Should Eval.init() or You have no Atoms")
        Eval.atoms(id) & Eval.mask
    }

    override def replace(env : Map[Int, Expr]) = this
    override def matching(parttern : Expr, env : Map[Int, Expr]) = parttern match {
            case a : Atom => id == a.id
            case s : Replacer => parttern.matching(this, env)
            case _ => false
    }
}


object Replacer {
    // global stats, performance consideration

    // a counter to counter how many replacer in replacing operation
    var counter = 0
    // a Set for Replacers in replacing operation
    val replacerSet = Set[Int]()
    def clear() = {
        replacerSet.clear()
        counter = 0
    }
}

class Replacer(val id : Int) extends Expr {
    // display
    def bracketed = true;
    override def toString() = "<" ++ id.toHexString + ">"

    // stats
    override val replacer_count = 1

    override def real_eval() = {
        throw ExprError("Replacer Can't Eval")
        0
    }

    override def search(i : Int, env : Map[Int, Expr]) = {
        if(id == i) true
        else if(env.isDefinedAt(id))
            env(id).search(i,env)
        else false
    }

    override def replace(env : Map[Int, Expr]) =
        if(env.isDefinedAt(id))
            env(id).replace(env)
        else {
            if(!Replacer.replacerSet.contains(this.id)){
                Replacer.replacerSet += this.id
                Replacer.counter += 1
            }
            this
        }
    
    override def matching(parttern : Expr, env : Map[Int, Expr]) = 
        if(env.isDefinedAt(this.id)) env(this.id).matching(parttern, env)
        else parttern match {
            case s : Replacer =>
                if(env.isDefinedAt(s.id))
                    matching(env(s.id), env)
                else { 
                    if(this != s) env(this.id) = s
                    true
                }
            case _ => 
                if(parttern.search(id,env)) // ! avoid the case that parttern contain this
                    false
                else {
                    env(this.id) = parttern
                    true
                }
        }

    def ===(that : Expr) =
        if(that.isInstanceOf[Replacer]) {
            val that0 = that.asInstanceOf[Replacer]
            id == that0.id
        } else false

}

