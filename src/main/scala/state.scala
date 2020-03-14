package prover.core

import prover.expr._
import scala.collection.mutable.{ArrayBuffer, PriorityQueue, Map, Set}
import scala.math.max


object State {
    type Axiom = Int => (Expr, Int)
    val L1Label = "[L1]"
    val L2Label = "[L2]"
    val L3Label = "[L3]"
    val MPLabel = "[MP]"
    val ASLabel = "[AS]"

    def L1(start : Int) = {
        val (p, q) = (\(start), \(start + 1))
        (p -> (q -> p), 2)  // to create the 2 replacer, you should add 2 to the replacer_index
    }
    def L2(start : Int) = {
        val (p, q, r) = (\(start), \(start + 1), \(start + 2))
        ((p -> (q -> r)) ->((p->q) -> (p->r)), 3)
    }
    def L3(start : Int) = {
        val (p, q) = (\(start), \(start + 1))
        ((~q -> ~p) -> (p -> q), 2)
    }
}

class State (
    var exprs : List[Expr] = Nil,
    val length :Int = 0
) extends Ordered[State] {
    var trace : List[String] = Nil

    // stats
    var replacer_index = 0  // for replacer generating
    var replacer_count = 0  // how many different replacer in the "exprs"
    var replacer_sum = 0    // how many times these replacers occur
    var not_depth = 0       // all expressions max not_depth
    var depth = 0           // all expressions max depth
    var in_not_depth = 0    // all expressions max in_not_depth
    var main_mp_level = 0   // how many times only [MP] (no [L1-3] [AS])

    // eval priorty for comparation
    def compare(that: State): Int = this.priorty compare that.priorty
    def relu(x:Int) = if(x > 0) x else 0
    def sqr(x : Int) = x * x
    def cube(x : Int) = x * x * x
    def quarter(x : Int) = x * x * x * x
    def priorty = - (
        length
        + 3 * sqr(relu(replacer_count - 1))
        + cube(relu(in_not_depth - 1))
        + cube(relu(replacer_sum - main_mp_level - 2))
        + cube(relu(depth/2 - 5))
        // + cube(relu(exprs.length/2 - 4))
    )


    // utils
    def ===(that : State) = this.exprs equals that.exprs
    override def toString(): String = "State: " ++ exprs.toString() ++
        s" with (${priorty} = f(${length}, ${3 * sqr(relu(replacer_count - 1))}, ${cube(relu(in_not_depth - 3))}))"

    
    private def TryExpr(state : State, expr: Expr, env : Map[Int, Expr]) = {
        // if we use the default env, we needs to clear it
        var ismatching = false
        Prover.env.clear()
        ismatching = exprs(0).matching(expr, Prover.env)
        if(ismatching && env != Prover.env){
            env ++= Prover.env
        }
        if(ismatching){
            var flag = true
            
            // get exprs
            Replacer.clear() // init for Replacer.counter and Replacer.replacers
            state.exprs = exprs.tail.map(_.replace(env))
            
            state.replacer_count = Replacer.counter
            state.main_mp_level = 0
            
            for(e <- state.exprs){
                if(e.replacer_count == 0){
                    if(!Eval(e)) flag = false
                    if(Prover.AvoidSet.contains(e)) flag = false
                }
            }
            // eval stats
            if(!state.exprs.isEmpty){
                state.not_depth = state.exprs.map(_.not_depth).max
                state.in_not_depth = state.exprs.map(_.in_not_depth).max
                state.replacer_sum = state.exprs.map(_.replacer_count).sum
                state.depth = state.exprs.map(_.depth).max
            }
            if(!Prover.tracing && Prover.assumption.length > 0)
                state.exprs = state.exprs.filter(!Prover.assumption.contains(_))
            
            // a restriction that snot_depth could not more than (initail not_depth) + 3
            flag &&= state.not_depth <= Prover.not_depth
            
            // Update Avoid Set
            if(flag && !state.exprs.isEmpty) 
                if(state.exprs(0).replacer_count == 0) Prover.AvoidSet += state.exprs(0)
            if(flag) Some(state) else None
        }else None
    }

    def TryAxiom(axiomf : State.Axiom, label : String, env:Map[Int, Expr] = Prover.env) : Option[State] = {
        val ret = new State(Nil,length + 1)
        val (axiom,len) = axiomf(replacer_index)
        ret.replacer_index = replacer_index + len

        // tracing only record which way they choose
        if(Prover.tracing)
            ret.trace = label +: trace
        
        TryExpr(ret, axiom, env)
    }

    def TryAssumption(env:Map[Int, Expr] = Prover.env) : Option[State] = {
        val state = new State(Nil, length + 1)
        
        state.replacer_index = replacer_index
        if(Prover.tracing)
            state.trace = State.ASLabel +: trace
        val ret = Prover.assumption
            .map(e => TryExpr(state, e, env))
            .find(_.isDefined)
        
        if(ret.isDefined) ret.get
        else None
    }

    def isCompleted() = exprs.length == 0

    def MP() = {
        val ret = new State(Nil,length + 1)
        
        if(Prover.tracing)
            ret.trace = State.MPLabel +: trace

        ret.not_depth = not_depth
        ret.main_mp_level = main_mp_level + 1
        ret.in_not_depth = in_not_depth
        
        val mpreplacer = new Replacer(replacer_index)
        ret.replacer_index = replacer_index + 1
        ret.replacer_count = replacer_count + 1

        val main_expr = (mpreplacer -> exprs(0))
        ret.exprs =  main_expr +: (mpreplacer +: exprs.tail)

        if(ret.main_mp_level <= Prover.max_main_mp_level) Some(ret) else None
    }

    def Next(que : PriorityQueue[State]) = {
        var ret : Option[State] = None

        def StateTry(l : Option[State], s: String) = {
            if(l.isDefined) {
                que += l.get
                // println(s"\t${s}: ${l}") // debug msg
                if(l.get.isCompleted) ret = l
            }
        }

        StateTry(TryAxiom(State.L1, State.L1Label) , State.L1Label )
        StateTry(TryAxiom(State.L2, State.L2Label) , State.L2Label )
        StateTry(TryAxiom(State.L3, State.L3Label) , State.L3Label )
        if(Prover.assumption.length > 0)
            StateTry(TryAssumption() ,State.ASLabel )
        StateTry(MP() , State.MPLabel)
        ret
    }
}