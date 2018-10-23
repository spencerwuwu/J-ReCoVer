// https://searchcode.com/api/result/63462/

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.impl.rtcp;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpBye extends RtcpCommonHeader {

	private long[] ssrcs = new long[31];

	protected RtcpBye() {

	}

	public RtcpBye(boolean padding) {
		super(padding, RtcpCommonHeader.RTCP_BYE);
	}

	protected int decode(byte[] rawData, int offSet) {

		int tmp = offSet;
		offSet = super.decode(rawData, offSet);

		for (int i = 0; i < this.count; i++) {
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
			this.ssrcs[i] <<= 8;
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
			this.ssrcs[i] <<= 8;
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
			this.ssrcs[i] <<= 8;
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
		}

		// Do we acre for optional part?

		return offSet;
	}

	protected int encode(byte[] rawData, int offSet) {

		int startPosition = offSet;

		offSet = super.encode(rawData, offSet);

		for (int i = 0; i < this.count; i++) {
			long ssrc = ssrcs[i];

			rawData[offSet++] = ((byte) ((ssrc & 0xFF000000) >> 24));
			rawData[offSet++] = ((byte) ((ssrc & 0x00FF0000) >> 16));
			rawData[offSet++] = ((byte) ((ssrc & 0x0000FF00) >> 8));
			rawData[offSet++] = ((byte) ((ssrc & 0x000000FF)));
		}
		
		/* Reduce 4 octest of header and length is in terms 32bits word */
		this.length = (offSet - startPosition - 4) / 4;

		rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
		rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));
		
		return offSet;
	}

	public void addSsrc(long ssrc) {
		this.ssrcs[this.count++] = ssrc;
	}

	public long[] getSsrcs() {
		return ssrcs;
	}

}

