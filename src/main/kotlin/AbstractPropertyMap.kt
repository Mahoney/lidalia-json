import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

abstract class AbstractPropertyMap<out V>(
    private val properties: MutableMap<String, V> = mutableMapOf()
) : Map<String, V> by properties {
    protected fun <V2 : @UnsafeVariance V> property(initialValue: V2) =
        PropertyDelegateProvider<Any, ReadOnlyProperty<Any, V2>> { _, prop ->
            properties[prop.name] = initialValue
            ReadOnlyProperty(properties::getValue)
        }
}
