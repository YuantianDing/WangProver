package prover.test

import prover.expr._
import prover.core._
import java.util.Date

object CoreTest extends App {
    val (p, q, r) = (Atom("p"), Atom("q"), Atom("r"))
    var start_time = new Date().getTime

    import prover.core.|-._

    |-.print_proof = true
    |-.print_length = 100

    // 基本结论（不含否定）
    |- (p -> p)
    List(p) |- (q -> p)
    List(q -> r) |- ((p -> q) -> (p -> r))
    List(p -> r) |- (p -> (q -> r))
    List(p -> q, p -> (q -> r)) |- (p -> r)
    List(p -> (q -> r)) |- (q -> (p -> r))
    List(p -> q, q -> r) |- (p -> r)

    // 作业题（不含否定）
    |- ( (p->q)->(p->p) )
    List(p, q -> (p -> r)) |- (q -> r)
    |- ( (p -> q) -> ((~p -> ~q) -> (q -> p)) )
    |- ( p-> (q -> (p -> q)) )
    |- (
        ((p -> (q -> r)) -> (p -> q)) ->
        ((p -> (q -> r)) -> (p -> r))
    )

    // 基本结论（含否定）
    List(~q, q) |- p
    List(~q) |- (q -> p)
    |- (~q -> (q -> p))
    List(~p -> p) |- p
    |- ((~p -> p) -> p)
    List(!p) |- p
    |- (!p -> p)
    List(p) |- !p
    |- (p -> !p)

    // 作业题（含否定）
    |- (~(p -> q) -> p)
    |- (~(p -> q) -> ~q)
    |- ( (~p -> ~q) -> ((~p -> q) -> p) )
    |- ( (p -> ~q) -> (q -> ~p) )
    |- ( (~p -> q) -> (~q -> p) )


    // 析取运算
    |- ( p -> (p | q) )
    |- ( q -> (p | q) )
    |- ( (p|q) -> (q|p) )
    |- ( (p|p) -> p )
    |- ( ~p | p)

    // 合取运算
    |- ( (p & q) -> p )
    |- ( (p & q) -> q )
    |- ( p -> (q & p) )
    |- ( q -> (q & p) )
    |- ( ~(p & ~p))

    var end_time =new Date().getTime
    println(s"total ${end_time-start_time} ms")
}