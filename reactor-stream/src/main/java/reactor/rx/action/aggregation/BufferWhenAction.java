/*
 * Copyright (c) 2011-2015 Pivotal Software Inc., Inc. All Rights Reserved.
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
package reactor.rx.action.aggregation;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.fn.Supplier;
import reactor.rx.action.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephane Maldini
 * @since 2.0
 */
public class BufferWhenAction<T> extends Action<T, List<T>> {

	private final List<T> values = new ArrayList<T>();
	private final Supplier<? extends Publisher<?>> boundarySupplier;

	public BufferWhenAction(Supplier<? extends Publisher<?>> boundarySupplier) {
		this.boundarySupplier = boundarySupplier;
	}

	@Override
	protected void doOnSubscribe(Subscription subscription) {
		super.doOnSubscribe(subscription);

		boundarySupplier.get().subscribe(new Subscriber<Object>() {

			Subscription s;

			@Override
			public void onSubscribe(Subscription s) {
				this.s = s;
				s.request(1);
			}

			@Override
			public void onNext(Object o) {
				flush();
				if (s != null) {
					s.request(1);
				}
			}

			@Override
			public void onError(Throwable t) {
				if (s != null) {
					s.cancel();
				}
				BufferWhenAction.this.onError(t);
			}

			@Override
			public void onComplete() {
				if (s != null) {
					s.cancel();
				}
				BufferWhenAction.this.onComplete();
			}
		});
	}

	private void flush() {
		List<T> toSend;
		synchronized (values) {
			if (values.isEmpty()) {
				return;
			}
			toSend = new ArrayList<T>(values);
			values.clear();
		}

		broadcastNext(toSend);
	}

	@Override
	protected void doError(Throwable ev) {
		synchronized (values) {
			values.clear();
		}
		super.doError(ev);
	}

	@Override
	protected void doComplete() {
		boolean last;
		synchronized (values){
			last = values.isEmpty();
		}
		if(!last){
			broadcastNext(values);
		}
		super.doComplete();
	}

	@Override
	protected void doNext(T value) {
		values.add(value);
	}


}
