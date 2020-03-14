package prover.expr

// ------------------------------------------------
// Eval
// ! can only evaluate 5 atoms (too many is useless)
// Usage:
//      Eval.assume(q)
//      Eval(p -> q)
// ------------------------------------------------

object Eval {
    val masks = Array(0x1, 0x3, 0xF, 0xFF, 0xFFFF, 0xFFFFFFFF)
    val atoms = Array(0xAAAAAAAA, 0xCCCCCCCC, 0xF0F0F0F0, 0xFF00FF00, 0xFFFF0000)

    var mask = 0
    var count = 0

    def init() : Unit = {
        count = Atom.symbols.length;
        if(count > 5) throw ExprError("Too Many Atoms!")
        mask = masks(count)
    }

    def apply(e : Expr) = (e.eval() & mask) == (0xFFFFFFFF & mask)

    def assume(exprs : Expr*) : Unit = {
        for(e <- exprs)
            mask = mask & e.eval()
    }
}
