package arrow.core.test.laws

import arrow.Kind
import arrow.core.Eval
import arrow.core.Option
import arrow.core.extensions.eq
import arrow.core.extensions.monoid
import arrow.core.extensions.option.eq.eq
import arrow.core.test.generators.GenK
import arrow.core.test.generators.functionAAToA
import arrow.core.test.generators.functionAToB
import arrow.core.test.generators.intSmall
import arrow.typeclasses.Applicative
import arrow.typeclasses.Eq
import arrow.typeclasses.EqK
import arrow.typeclasses.Reducible
import io.kotest.property.Arb
import io.kotlintest.properties.forAll

object ReducibleLaws {

  fun <F> laws(RF: Reducible<F>, GA: Applicative<F>, GENK: GenK<F>, EQK: EqK<F>): List<Law> {

    val EQ = Int.eq()
    val EQOptionInt = Option.eq(Int.eq())
    val EQLong = Long.eq()
    val G = GENK.genK(Arb.int())

    return FoldableLaws.laws(RF, GA, GENK, EQK) +
      listOf(
        Law("Reducible Laws: reduceLeftTo consistent with reduceMap") { RF.reduceLeftToConsistentWithReduceMap(G, EQ) },
        Law("Reducible Laws: reduceRightTo consistent with reduceMap") { RF.reduceRightToConsistentWithReduceMap(G, EQ) },
        Law("Reducible Laws: reduceRightTo consistent with reduceRightToOption") { RF.reduceRightToConsistentWithReduceRightToOption(G, EQOptionInt) },
        Law("Reducible Laws: reduceRight consistent with reduceRightOption") { RF.reduceRightConsistentWithReduceRightOption(G, EQOptionInt) },
        Law("Reducible Laws: reduce reduce left consistent") { RF.reduceReduceLeftConsistent(G, EQ) },
        Law("Reducible Laws: size consistent") { RF.sizeConsistent(G, EQLong) }
      )
  }

  fun <F> Reducible<F>.reduceLeftToConsistentWithReduceMap(cf: Arb<Kind<F, Int>>, EQ: Eq<Int>) =
    forAll(Arb.functionAToB<Int, Int>(Arb.intSmall()), cf) { f: (Int) -> Int, fa: Kind<F, Int> ->
      with(Int.monoid()) {
        fa.reduceMap(this, f).equalUnderTheLaw(fa.reduceLeftTo(f) { b, a -> b.combine(f(a)) }, EQ)
      }
    }

  fun <F> Reducible<F>.reduceRightToConsistentWithReduceMap(cf: Arb<Kind<F, Int>>, EQ: Eq<Int>) =
    forAll(Arb.functionAToB<Int, Int>(Arb.intSmall()), cf) { f: (Int) -> Int, fa: Kind<F, Int> ->
      with(Int.monoid()) {
        fa.reduceMap(this, f).equalUnderTheLaw(fa.reduceRightTo(f) { a, eb -> eb.map { f(a).combine(it) } }.value(), EQ)
      }
    }

  fun <F> Reducible<F>.reduceRightToConsistentWithReduceRightToOption(cf: Arb<Kind<F, Int>>, EQ: Eq<Option<Int>>) =
    forAll(Arb.functionAToB<Int, Int>(Arb.intSmall()), cf) { f: (Int) -> Int, fa: Kind<F, Int> ->
      with(Int.monoid()) {
        fa.reduceRightToOption(f) { a, eb -> eb.map { f(a).combine(it) } }.value()
          .equalUnderTheLaw(fa.reduceRightTo(f) { a, eb -> eb.map { f(a).combine(it) } }.map { Option(it) }.value(), EQ)
      }
    }

  fun <F> Reducible<F>.reduceRightConsistentWithReduceRightOption(cf: Arb<Kind<F, Int>>, EQ: Eq<Option<Int>>) =
    forAll(Arb.functionAAToA(Arb.intSmall()), cf) { f: (Int, Int) -> Int, fa: Kind<F, Int> ->
      fa.reduceRight { a1, e2 -> Eval.Now(f(a1, e2.value())) }.map { Option(it) }.value()
        .equalUnderTheLaw(fa.reduceRightOption { a1, e2 -> Eval.Now(f(a1, e2.value())) }.value(), EQ)
    }

  fun <F> Reducible<F>.reduceReduceLeftConsistent(cf: Arb<Kind<F, Int>>, EQ: Eq<Int>) =
    forAll(cf) { fa: Kind<F, Int> ->
      with(Int.monoid()) {
        fa.reduce(this).equalUnderTheLaw(fa.reduceLeft { a1, a2 -> a1.combine(a2) }, EQ)
      }
    }

  fun <F> Reducible<F>.sizeConsistent(cf: Arb<Kind<F, Int>>, EQ: Eq<Long>) =
    forAll(cf) { fa: Kind<F, Int> ->
      with(Long.monoid()) {
        fa.size(this).equalUnderTheLaw(fa.reduceMap(this) { 1L }, EQ)
      }
    }
}
