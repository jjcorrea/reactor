/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.core.action;

import reactor.core.Observable;
import reactor.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephane Maldini
 * @since 1.1
 */
public class CollectAction<T> extends BatchAction<T> implements Flushable<T> {

	private final List<T> values;

	public CollectAction(int batchsize, Observable d, Object successKey, Object failureKey) {
		super(batchsize, d, successKey, failureKey);
		values = new ArrayList<T>(batchsize > 0 ? batchsize : 256);
	}

	@Override
	public void doNext(Event<T> value) {
		values.add(value.getData());
	}

	@Override
	public void doFlush(Event<T> ev) {
		if (values.isEmpty()) {
			return;
		}
		notifyValue(Event.wrap(new ArrayList<T>(values)));
		values.clear();
	}

	@Override
	public Flushable<T> flush() {
		lock.lock();
		try {
			doFlush(null);
		} finally {
			lock.unlock();
		}
		return this;
	}

}
