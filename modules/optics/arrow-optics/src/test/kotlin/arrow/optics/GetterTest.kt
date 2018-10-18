package arrow.optics

import arrow.core.*
import arrow.data.State
import arrow.data.k
import arrow.data.map
import arrow.data.run
import arrow.instances.monoid
import arrow.test.UnitSpec
import arrow.test.generators.genFunctionAToB
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class GetterTest : UnitSpec() {

  init {

    val userGetter = userIso.asGetter()
    val length = Getter<String, Int> { it.length }
    val upper = Getter<String, String> { it.toUpperCase() }

    with(tokenGetter.asFold()) {

      "asFold should behave as valid Fold: size" {
        forAll(TokenGen) { token ->
          size(token) == 1
        }
      }

      "asFold should behave as valid Fold: nonEmpty" {
        forAll(TokenGen) { token ->
          nonEmpty(token)
        }
      }

      "asFold should behave as valid Fold: isEmpty" {
        forAll(TokenGen) { token ->
          !isEmpty(token)
        }
      }

      "asFold should behave as valid Fold: getAll" {
        forAll(TokenGen) { token ->
          getAll(token) == listOf(token.value).k()
        }
      }

      "asFold should behave as valid Fold: combineAll" {
        forAll(TokenGen) { token ->
          combineAll(String.monoid(), token) == token.value
        }
      }

      "asFold should behave as valid Fold: fold" {
        forAll(TokenGen) { token ->
          fold(String.monoid(), token) == token.value
        }
      }

      "asFold should behave as valid Fold: headOption" {
        forAll(TokenGen) { token ->
          headOption(token) == Some(token.value)
        }
      }

      "asFold should behave as valid Fold: lastOption" {
        forAll(TokenGen) { token ->
          lastOption(token) == Some(token.value)
        }
      }
    }

    with(tokenGetter) {

      "Getting the target should always yield the exact result" {
        forAll { value: String ->
          get(Token(value)) == value
        }
      }

      "Finding a target using a predicate within a Getter should be wrapped in the correct option result" {
        forAll { value: String, predicate: Boolean ->
          find(Token(value)) { predicate }.fold({ false }, { true }) == predicate
        }
      }

      "Checking existence of a target should always result in the same result as predicate" {
        forAll { value: String, predicate: Boolean ->
          exist(Token(value)) { predicate } == predicate
        }
      }
    }

    "Zipping two lenses should yield a tuple of the targets" {
      forAll { value: String ->
        length.zip(upper).get(value) == value.length toT value.toUpperCase()
      }
    }

    "Joining two getters together with same target should yield same result" {
      val userTokenStringGetter = userGetter compose tokenGetter
      val joinedGetter = tokenGetter.choice(userTokenStringGetter)

      forAll { tokenValue: String ->
        val token = Token(tokenValue)
        val user = User(token)
        joinedGetter.get(Left(token)) == joinedGetter.get(Right(user))
      }
    }

    "Pairing two disjoint getters should yield a pair of their results" {
      val splitGetter: Getter<Tuple2<Token, User>, Tuple2<String, Token>> = tokenGetter.split(userGetter)
      forAll(TokenGen, UserGen) { token: Token, user: User ->
        splitGetter.get(token toT user) == token.value toT user.token
      }
    }

    "Creating a first pair with a type should result in the target to value" {
      val first = tokenGetter.first<Int>()
      forAll(TokenGen, Gen.int()) { token: Token, int: Int ->
        first.get(token toT int) == token.value toT int
      }
    }

    "Creating a second pair with a type should result in the value target" {
      val first = tokenGetter.second<Int>()
      forAll(Gen.int(), TokenGen) { int: Int, token: Token ->
        first.get(int toT token) == int toT token.value
      }
    }

    "Extract should extract the focus from the state" {
      forAll(TokenGen) { token ->
        tokenGetter.extract().run(token) ==
          State { token: Token ->
            token toT tokenGetter.get(token)
          }.run(token)
      }
    }

    "toState should be an alias to extract" {
      forAll(TokenGen) { token ->
        tokenGetter.toState().run(token) == tokenGetter.extract().run(token)
      }
    }

    "extractMap with f should be same as extract and map" {
      forAll(TokenGen, genFunctionAToB<String, String>(Gen.string())) { token, f ->
        tokenGetter.extractMap(f).run(token) == tokenGetter.extract().map(f).run(token)
      }
    }

  }

}
