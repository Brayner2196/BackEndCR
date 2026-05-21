package com.backendcr.residentialcomplex.config.multitenant;

public class TenantContext {

	private static final ThreadLocal<String> currentTenant   = new ThreadLocal<>();
	private static final ThreadLocal<String> currentTimezone = new ThreadLocal<>();

	public static void setTenant(String tenant) {
		currentTenant.set(tenant);
	}

	public static String getTenant() {
		return currentTenant.get();
	}

	public static void setTimezone(String timezone) {
		currentTimezone.set(timezone);
	}

	/** Retorna la timezone del tenant activo, o "America/Bogota" si no está seteada. */
	public static String getTimezone() {
		String tz = currentTimezone.get();
		return (tz != null && !tz.isBlank()) ? tz : "America/Bogota";
	}

	public static void clear() {
		currentTenant.remove();
		currentTimezone.remove();
	}
}
