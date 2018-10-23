// https://searchcode.com/api/result/3138412/

package honeycrm.client.prefetch;

import java.io.Serializable;
import java.util.Arrays;

public class CacheKey implements Serializable {
	private static final long serialVersionUID = 7799103367359530032L;
	private Object[] parameters;

	public CacheKey() {
	}

	public CacheKey(final Object... pars) {
		this.parameters = new Object[pars.length];
		for (int i = 0; i < pars.length; i++) {
			this.parameters[i] = pars[i];
		}
	}

	/**
	 * Two cache entries are equals if they store the same parameters.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CacheKey) {
			CacheKey e = (CacheKey) obj;
			if (e.getParameters().length == this.getParameters().length) {
				return Arrays.equals(e.getParameters(), this.getParameters());
			}
		}
		return false;
	}

	/**
	 * TODO improve this to avoid collisions in order to reduce the number of necessary equals calls
	 */
	@Override
	public int hashCode() {
		return 0;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

}

