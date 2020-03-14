package prover.core

import prover.expr._
import scala.collection.mutable.{ArrayBuffer, PriorityQueue, Map, Set}
import scala.math.max

// ------------------------------------
// Prover
// Usage:
//      Prover.assume(p)
//      Prover.prove(q -> p)
// ------------------------------------

object Prover {
    // configuration
    var max_main_mp_level = 2
    var max_not_depth_diff = 3 // not_depth limitation 
    var tracing = false
    // global varibles
    var not_depth = 0
    val env = Map[Int, Expr]()
    var AvoidSet = Set[Expr]()
    var assumption = ArrayBuffer[Expr]()
    var target :Expr = new Replacer(0)

    def assume(expr : Expr*) = {
        Eval.init()
        Eval.assume(expr: _*)
        assumption.clear()
        assumption ++= expr
        not_depth = 0
        for(e <- expr)
            not_depth = max(not_depth, 
                if(e.not_depth == 0) 0 else e.not_depth + Prover.max_not_depth_diff
            )
    }

    def prove(e : Expr) = {
        if(Eval(e)) {
            target = e
            not_depth = max(not_depth,
                if(e.not_depth == 0) 0 else e.not_depth + Prover.max_not_depth_diff
            )
            AvoidSet += e
            val state0 = new State(List(e))
            val que = PriorityQueue[State](state0)
            var ans : Option[State] = None
            
            var loop = 0
            while(!ans.isDefined) {
                val top = que.dequeue()
                // println(top)
                ans = top.Next(que)
                loop += 1
                if(loop % 50000 == 0) {
                    println(s"[info] proved for $loop cases")
                }
            }
            println(s"[info] proved for total $loop cases")
            AvoidSet.clear()
            ans
        } else None
    }


}

// ------------------------------------------------------------
// object |-
// frontend of the whole core
// ------------------------------------------------------------

object |- {
    // whether use |- to print proof
    var print_proof = false
    var print_length = 50
    implicit class Snippets(L : List[Expr]) {
        def |-(e : Expr) = {
            println(s"[info] prove for $e with assumption $L")
            Prover.assume(L: _*)

            val save_tracing = Prover.tracing // save Prover.tracing
            Prover.tracing = print_proof 

            val ret = Prover.prove(e)


            if(ret.isDefined){
                println(s"[info] the length of the proof of $e : ${ret.get.length}")
                if(print_proof) Proof(ret).show(print_length)
            } else {
                println("[info] expression are wrong")
            }
            Prover.tracing = save_tracing // recover Prover.tracing
            ret
        }
    }
    def apply(e : Expr) = {
        List[Expr]() |- e
    }
}