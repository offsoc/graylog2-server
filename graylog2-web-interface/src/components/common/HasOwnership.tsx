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

import { createGRN } from 'logic/permissions/GRN';
import useCurrentUser from 'hooks/useCurrentUser';

type ChildFun = (props: { disabled: boolean }) => React.ReactElement;

type Props = {
  children: React.ReactNode | ChildFun;
  id?: string;
  type: string;
  hideChildren?: boolean;
};

const HasOwnership = ({ children, id, type, hideChildren = false }: Props) => {
  const currentUser = useCurrentUser();
  const entity = createGRN(type, id);
  const ownership = `entity:own:${entity}`;
  const adminPermission = '*';

  if (currentUser) {
    const { grnPermissions = [], permissions } = currentUser;
    const isAdmin = permissions.includes(adminPermission);

    if (grnPermissions.includes(ownership) || isAdmin) {
      if (!hideChildren && typeof children === 'function') {
        return <>{children({ disabled: false })} </>;
      }

      return <>children</>;
    }
  }

  if (!hideChildren && typeof children === 'function') {
    return <>{children({ disabled: true })} </>;
  }

  return null;
};

export default HasOwnership;
