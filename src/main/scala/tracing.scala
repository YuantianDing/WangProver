package prover.core

import prover.expr._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{ArrayBuffer, PriorityQueue, Map, Set}
import scala.math.max

object Proof {
    def apply(s : Option[State]) = {
        new Proof(s.get)
    }
}

class Proof(s : State) {
    if(!Prover.tracing)
        throw ExprError("You Should Enable Tracing to Generate Proof")
    val trace = s.trace
    val labels = trace.toArray.reverse

    val proof = ArrayBuffer[Expr]()
    var state = new State(List(Prover.target))
    val env = Map[Int, Expr]()
    iter(0)
    def iter(i : Int) : Unit = {
        if(i < labels.length){
            val expr = state.exprs(0)
            labels(i) match {
                case State.L1Label => state = state.TryAxiom(State.L1, State.L1Label, env).get
                case State.L2Label => state = state.TryAxiom(State.L2, State.L2Label, env).get
                case State.L3Label => state = state.TryAxiom(State.L3, State.L3Label, env).get
                case State.ASLabel => state = state.TryAssumption(env).get
                case State.MPLabel => state = state.MP().get
            }
            iter(i+1)
            proof += expr.replace(env)
        }
    }
    def show(len:Int = 150) = {
        var i = 0;
        for((e, l) <- proof zip trace) {
            var str = e.toString()
            while(str.length > len){
                println(f"${i+1}%5d: " ++ str.substring(0, len))
                str = str.substring(len)
            }
            println(f"${i+1}%5d: " ++ str ++ (" " * (len - str.length)) ++ l)
            i += 1;
        }
    }
    
}