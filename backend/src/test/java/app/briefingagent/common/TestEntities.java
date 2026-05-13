package app.briefingagent.common;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

/**
 * Tiny test helper that lets us simulate the JPA-assigned identifier and
 * audit timestamps for entities passed through services in unit tests (i.e.
 * without going via the persistence layer). Walks the class hierarchy to
 * find declared fields on {@link BaseEntity} or {@link AuditedEntity}.
 */
public final class TestEntities {

    private TestEntities() {
    }

    public static <T> T withId(T entity, UUID id) {
        assignField(entity, "id", id);
        markCreated(entity);
        return entity;
    }

    public static <T> T withRandomId(T entity) {
        return withId(entity, UUID.randomUUID());
    }

    private static void markCreated(Object entity) {
        Instant now = Instant.now();
        assignFieldIfPresent(entity, "createdAt", now);
        assignFieldIfPresent(entity, "updatedAt", now);
    }

    private static void assignField(Object entity, String fieldName, Object value) {
        if (!assignFieldIfPresent(entity, fieldName, value)) {
            throw new IllegalStateException(
                    "No '" + fieldName + "' field found on " + entity.getClass());
        }
    }

    private static boolean assignFieldIfPresent(Object entity, String fieldName, Object value) {
        Class<?> cursor = entity.getClass();
        while (cursor != null && cursor != Object.class) {
            try {
                Field f = cursor.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(entity, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot set " + fieldName + " reflectively", e);
            }
        }
        return false;
    }
}
