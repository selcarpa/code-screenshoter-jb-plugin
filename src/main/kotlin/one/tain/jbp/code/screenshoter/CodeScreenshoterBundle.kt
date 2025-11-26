package one.tain.jbp.code.screenshoter

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/** The resource bundle name for the code screenshoter plugin */
@NonNls
private const val BUNDLE = "messages.CodeScreenshoterBundle"

/**
 * Object for managing localized messages for the code screenshoter plugin.
 * This class provides access to internationalized strings that are used throughout
 * the plugin, supporting multiple languages as defined in the resource bundle files.
 */
object CodeScreenshoterBundle : DynamicBundle(BUNDLE) {

    /**
     * Retrieves a localized message string with the specified key.
     * This method fetches the appropriate message from the resource bundle,
     * applying any provided parameters to format the message.
     *
     * @param key The property key for the message to retrieve
     * @param params Optional parameters to format the message with
     * @return The localized message string
     */
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

    /**
     * Retrieves a lazy message pointer for the specified key.
     * This method creates a lazy message that will be evaluated only when needed,
     * which can be more efficient for messages that might not be displayed.
     *
     * @param key The property key for the message to retrieve
     * @param params Optional parameters to format the message with
     * @return A message pointer that can be resolved to the localized string
     */
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getLazyMessage(key, *params)
}
