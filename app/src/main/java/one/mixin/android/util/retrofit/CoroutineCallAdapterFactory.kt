package one.mixin.android.util.retrofit

import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class CoroutineCallAdapterFactory private constructor() : CallAdapter.Factory() {
  companion object {
    @JvmStatic @JvmName("create")
    operator fun invoke() = CoroutineCallAdapterFactory()
  }

  override fun get(
      returnType: Type,
      annotations: Array<out Annotation>,
      retrofit: Retrofit
  ): CallAdapter<*, *>? {
    if (Deferred::class.java != getRawType(returnType)) {
      return null
    }
    if (returnType !is ParameterizedType) {
      throw IllegalStateException(
          "Deferred return type must be parameterized as Deferred<Foo> or Deferred<out Foo>")
    }
    val responseType = getParameterUpperBound(0, returnType)

    val rawDeferredType = getRawType(responseType)
    return if (rawDeferredType == Response::class.java) {
      if (responseType !is ParameterizedType) {
        throw IllegalStateException(
            "Response must be parameterized as Response<Foo> or Response<out Foo>")
      }
      ResponseCallAdapter<Any>(getParameterUpperBound(0, responseType))
    } else {
      BodyCallAdapter<Any>(responseType)
    }
  }

  private class BodyCallAdapter<T>(
      private val responseType: Type
  ) : CallAdapter<T, Deferred<T>> {

    override fun responseType() = responseType

    override fun adapt(call: Call<T>): Deferred<T> {
      val deferred = CompletableDeferred<T>()

      deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
          call.cancel()
        }
      }

      call.enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
          deferred.completeExceptionally(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
          if (response.isSuccessful) {
            deferred.complete(response.body()!!)
          } else {
            deferred.completeExceptionally(HttpException(response))
          }
        }
      })

      return deferred
    }
  }

  private class ResponseCallAdapter<T>(
      private val responseType: Type
  ) : CallAdapter<T, Deferred<Response<T>>> {

    override fun responseType() = responseType

    override fun adapt(call: Call<T>): Deferred<Response<T>> {
      val deferred = CompletableDeferred<Response<T>>()

      deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
          call.cancel()
        }
      }

      call.enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
          deferred.completeExceptionally(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
          deferred.complete(response)
        }
      })

      return deferred
    }
  }
}