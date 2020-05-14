<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=default"></script>

# WangProver 证明器程序原理说明

证明器编写的方法有很多，其中一个常用的方法是从结论到假设的反向证明，被成为 Resolution 方法，本证明器采用这种方法，并采用 A* 算法进行进一步加速，目前能够大体上完成 50 行以下的证明（部分五十行以上的证明）

## Resolution 方法

为什么要从结论到假设进行反向证明呢？因为当前证明的大部分定理（结论）中，结论是非常重要的，试想，如果不通过结论反向证明，就只能利用已有公理、假设进行暴力遍历，虽然这样能够遍历到所有公式，但是效率上会低很多。Resolution 方法这种反向的方法遍历的空间虽然也是无限的，但从结论出发反向遍历可以避免遍历到大量无用的结论。

我们通过同一律的例子来看 Resolution 如何工作：

首先先把 L(X) 中的基本规则放在这里：

$$
\begin{align}
L1 &: p \rightarrow (q \rightarrow p)\\
L2 &: (p \rightarrow (q \rightarrow r)) \rightarrow ((p\rightarrow q) \rightarrow (p \rightarrow r)) \\
L3 &: (¬p \rightarrow ¬q) \rightarrow (q \rightarrow p) \\
MP &: \{ p, p \rightarrow q\} \vdash q
\end{align}
$$

证明 $p \rightarrow p$ ：

<img src="/Users/dnailz/Course/MathematicalLogic/prover/doc.assets/IMG_1002AFDBE04B-1.jpeg" alt="IMG_1002AFDBE04B-1" style="zoom:50%;" />

1. 首先，$p \rightarrow p$ 显然不是$L1$ $L2$ $L3$ 中的公式，故它一定是$MP$ 得来的。

    故设存在一个 $A$ 使得 $\{A, A \rightarrow (p \rightarrow p)\} \vdash p \rightarrow p$

2. 那么如何证明 $\{A, A \rightarrow (p \rightarrow p)\}$ 呢， 这两个式子相比较而言 $A \rightarrow (p \rightarrow p)$ 信息更丰富一些，故进一步研究 $A \rightarrow (p \rightarrow p)$ 。

    这样 $A \rightarrow (p \rightarrow p)$ 可以是$L1$，但是这样的话我们要证明$p$， 它是一个偶然式，不可证明，故舍去这种可能。

    另一方面，$A \rightarrow (p \rightarrow p)$  可以由$MP$  得到，再设存在$B$ 使得 $\{A, B, B\rightarrow (A \rightarrow (p \rightarrow p))\}$  成立。
    
3. 进一步，如何证明 $\{A, B, B\rightarrow (A \rightarrow (p \rightarrow p))\}$ 呢， 我们观察$B\rightarrow (A \rightarrow (p \rightarrow p))$ 发现：

    $B\rightarrow (A \rightarrow (p \rightarrow p))$ 作为$L2$ 非常合适，这样的话，如果令$B = p \rightarrow (C \rightarrow p)$ ，$A = p \rightarrow C$  发现此时，$B\rightarrow (A \rightarrow (p \rightarrow p))$ 就是$L2$ 。
    
4. 接下来，证明 $\{ p \rightarrow (C \rightarrow p), p \rightarrow C\}$  就行了，$p \rightarrow (C \rightarrow p)$ 本身构成$L1$ ，故可以直接忽略，$p \rightarrow C$ 可以通过令 $C=D  \rightarrow p$ 的方式构成 $L1$ 故证明完成。

我的证明器实现了这种方法（通过BFS，因为显然DFS会搜不到），反向输出证明如下：

```
[info] prove for p -> p with assumption List()
[info] proved for total 5 cases
[info] the length of the proof of p -> p : 5
    1: p -> (<8> -> p)                                   [L1]
    2: p -> ((<8> -> p) -> p)                            [L1]
    3: (p -> ((<8> -> p) -> p)) -> ((p -> (<8> -> p)) -> 
    3: (p -> p))                                         [L2]
    4: (p -> (<8> -> p)) -> (p -> p)                     [MP2,3]
    5: p -> p                                            [MP1,4]
```

这里 `<8>` 是一个非常奇怪的符号，它根上面证明的 $D$ 含义相同，表示任意公式，这里可以代入 $p$ 也可以代入 $p \rightarrow p$ 甚至随便你想要的公式，都可以完成证明，当然，课本使用的是 $p$。

从这里我们可以看到，其实 Resolution 方法非常符合人的证明习惯，如果是我的话，我可能会想着这样证明：

<img src="/Users/dnailz/Course/MathematicalLogic/prover/doc.assets/image-20200514142427312.png" alt="image-20200514142427312" style="zoom:33%;" />

自行观察就会发现，Resolution方法与人对这种定理的证明非常相似。

## A* 算法优化

### BFS 下的搜索算法的性能

BFS 暂时还是比较慢，搜索个简单的 `(¬p -> p) -> p` 对于我的笔记本电脑来说会跑个好几秒钟，像 `¬(p -> q) -> p` 则需要更长的时间（虽然还是能搜出来一个结果），所以还是需要进一步优化。

### 采用 A* 算法的优化

仔细观察程序的运行过程后，发现有大量的代码显然没有意义：

1. 有很多公式有大量的待定符号（像`<8>`），这种符号过多让人感觉有一种过度猜测的感觉，对整体的运行非常不好。
2. 出现了大量的没有必要的否定词，一般而言，公式不应当有否定词中包含过多东西的情况，如 `¬(p -> (q -> r))` 这种在实际的证明中会非常难处理，必须对这种的使用加一一定的限制。
3. 对过长的公式也应当有一定的限制。

于是改 BFS 为 A* 算法，这样做虽然不能保证找出来的证明式最短的，但效率上可以通过对以上描述的公式给以一些比较差的优先级，然后优先选择备选方案中较优的优先处理。

这里使用 Scala 的 `PriorityQueue` 实现，经过一段时间的调参，得到具体的优先级函数如下：

```scala
    def priorty = - (
        length                                  // 证明的长度
        + 3 * sqr(relu(replacer_count - 1))     // 待定元素的数量
        + cube(relu(in_not_depth - 1))          // 否定词中表达式的深度
        + cube(relu(replacer_sum - main_mp_level - 2))
        + cube(relu(depth/2 - 5))
    )
    // sqr 是平方，cube是立方，relu是relu函数
```

这样会比原来BFS的运行效率提高 10 倍左右，现在运行 maintest.scala （包含一堆复杂的公式），1-2秒就可以得到全部结果。

## 其他一些优化

1. 其实发现 MP 规则不会连续使用超过 3 次，故会对MP规则的大量连续使用做出限制。
2. 实际上大多数不含否定词的公式，都不需要使用 L3，故对 不含否定词的问题，会限制L3的使用。
3. 会检测并排出出现重复公式的情况。

## Scala 与函数式编程

### 领域特定语言（DSL）

我不想自己写一个 parser，那样会比较麻烦（至少在过程式语言中是如此，Haskell没太用过），所以就直接使用 Scala 的 DSL 功能实现对公式的输入，比如这样的一个小程序，就利用领域特定语言自动地完成解析的工作。

```scala
package prover.test

import prover.expr._
import prover.core._

object MyTest extends App {
    import prover.core.|-._

    val p = Atom("p")
    |- (p -> p)
    |- (~p -> p) -> p
}
```

### 函数式编程

Scala对函数式支持比较友好，不像某人生苦短语言（有 coconut 也还能用，可以尝试），Scala写一些模式匹配会方便一些。

不过，非常惭愧，由于一开始不是特别了解 Scala，很多函数式特性我的代码中并没有用好（函数式不是很熟也式我用Scala的原因）。