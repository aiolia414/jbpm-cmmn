package org.jbpm.cmmn.casefile.jcr;

public class JcrUtil {
	public static RuntimeException convertException(Exception e) {
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		} else {
			return new RuntimeException(e);
		}
	}
}
