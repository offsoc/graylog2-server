/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import selectEvent from 'helpers/selectEvent';

import StreamsFilter from './StreamsFilter';

describe('StreamsFilter', () => {
  it('sorts stream names', async () => {
    const streams = [
      { key: 'One Stream', value: 'streamId1' },
      { key: 'another Stream', value: 'streamId2' },
      { key: 'Yet another Stream', value: 'streamId3' },
      { key: '101 Stream', value: 'streamId4' },
    ];
    render(<StreamsFilter streams={streams} onChange={() => {}} />);

    const select = await screen.findByLabelText(/select streams/i);

    selectEvent.openMenu(select);

    await screen.findByText('101 Stream');
    await screen.findByText('another Stream');
    await screen.findByText('One Stream');
    await screen.findByText('Yet another Stream');
  });
});
