package be.winnetrie.mod.simpleknapping.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Keeps physical-client classes out of common networking classes.
 *
 * NeoForge 1.21.1 registers bidirectional handlers from common code. Direct
 * method references to classes that use net.minecraft.client can make those
 * classes visible to the dedicated-server class loader. This bridge only
 * resolves the real client handler when a clientbound payload is actually
 * handled on a physical client.
 */
public final class ClientPayloadBridge {
    private static final String CLIENT_PACKAGE =
            "be.winnetrie.mod.simpleknapping.client.";

    private ClientPayloadBridge() {
    }

    public static void handleRecipeEditor(RecipeEditorPayload payload, IPayloadContext context) {
        invokeClientHandler(
                CLIENT_PACKAGE + "RecipeEditorClientPayloadHandler",
                RecipeEditorPayload.class,
                payload,
                context
        );
    }

    public static void handleSettings(SettingsPayload payload, IPayloadContext context) {
        invokeClientHandler(
                CLIENT_PACKAGE + "SettingsClientPayloadHandler",
                SettingsPayload.class,
                payload,
                context
        );
    }

    public static void handleRecipeGuide(RecipeGuidePayload payload, IPayloadContext context) {
        invokeClientHandler(
                CLIENT_PACKAGE + "RecipeGuideClientPayloadHandler",
                RecipeGuidePayload.class,
                payload,
                context
        );
    }

    private static <T> void invokeClientHandler(
            String className,
            Class<T> payloadType,
            T payload,
            IPayloadContext context
    ) {
        try {
            Class<?> handlerClass = Class.forName(className);
            Method handle = handlerClass.getMethod("handle", payloadType, IPayloadContext.class);
            handle.invoke(null, payload, context);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Client payload handler was invoked where client classes are unavailable: " + className,
                    e
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Invalid client payload handler: " + className, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Client payload handler failed: " + className, cause);
        }
    }
}
