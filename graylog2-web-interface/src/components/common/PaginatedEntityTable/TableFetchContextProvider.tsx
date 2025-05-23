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
import { useMemo } from 'react';

import type { SearchParams, Attribute } from 'stores/PaginationTypes';

import TableFetchContext from './TableFetchContext';

type Props = React.PropsWithChildren<{
  attributes: Array<Attribute>;
  refetch: () => void;
  searchParams: SearchParams;
  entityTableId: string;
}>;

const TableFetchContextProvider = ({
  children = undefined,
  searchParams,
  refetch,
  attributes,
  entityTableId,
}: Props) => {
  const contextValue = useMemo(
    () => ({
      searchParams,
      refetch,
      attributes,
      entityTableId,
    }),
    [searchParams, refetch, attributes, entityTableId],
  );

  return <TableFetchContext.Provider value={contextValue}>{children}</TableFetchContext.Provider>;
};

export default TableFetchContextProvider;
