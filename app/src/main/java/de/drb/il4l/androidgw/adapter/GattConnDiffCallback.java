/*
 * Copyright (c) 2018, Nordic Semiconductor
 * Copyright (c) 2022, Dr. Brunthaler GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.drb.il4l.androidgw.adapter;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import de.drb.il4l.androidgw.viewmodels.GattConnection;

public class GattConnDiffCallback extends DiffUtil.Callback {
	private final List<GattConnection> oldList;
	private final List<GattConnection> newList;

	public GattConnDiffCallback(final List<GattConnection> oldList,
                         final List<GattConnection> newList) {
		this.oldList = oldList;
		this.newList = newList;
	}

	@Override
	public int getOldListSize() {
		return oldList != null ? oldList.size() : 0;
	}

	@Override
	public int getNewListSize() {
		return newList != null ? newList.size() : 0;
	}

	@Override
	public boolean areItemsTheSame(final int oldItemPosition, final int newItemPosition) {
		GattConnection a = oldList != null ? oldList.get(oldItemPosition) : null;
		GattConnection b = newList.get(oldItemPosition);
		return a == b;
	}

	@Override
	public boolean areContentsTheSame(final int oldItemPosition, final int newItemPosition) {
		boolean connStateSame = (newList.get(newItemPosition).getConnectionState() == oldList.get(oldItemPosition).getConnectionState());
		boolean uRateSame = (newList.get(newItemPosition).getURate() == oldList.get(oldItemPosition).getURate());
		return (connStateSame && uRateSame);
	}

	@Nullable
	@Override
	public Object getChangePayload(int oldItemPosition, int newItemPosition) {
		// Implement method if you're going to use ItemAnimator
		return super.getChangePayload(oldItemPosition, newItemPosition);
	}

}
