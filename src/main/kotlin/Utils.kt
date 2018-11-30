import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.cdimascio.dotenv.Dotenv

val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()!!

val moshiAdapter = moshi.adapter(Any::class.java)!!

fun Any?.toJson(indent: String = "") = moshiAdapter.indent(indent).toJson(this)!!

fun Dotenv.mustGet(name: String) = this[name] ?: throw Exception("environment variable '$name' not set")

