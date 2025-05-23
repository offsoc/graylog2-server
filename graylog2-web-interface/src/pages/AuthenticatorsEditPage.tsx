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
import React from 'react';

import AuthenticatorActionLinks from 'components/authentication/AuthenticatorActionLinks';
import AuthenticatorsEdit from 'components/authentication/AuthenticatorsEdit';
import { PageHeader, DocumentTitle } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import AuthenticationPageNavigation from 'components/authentication/AuthenticationPageNavigation';

const AuthenticatorsEditPage = () => (
  <DocumentTitle title="Edit Authenticators">
    <AuthenticationPageNavigation />
    <PageHeader
      title="Edit Authenticators"
      actions={<AuthenticatorActionLinks />}
      documentationLink={{
        title: 'Authenticators documentation',
        path: DocsHelper.PAGES.AUTHENTICATORS,
      }}>
      <span>Configure the trusted header authentication.</span>
    </PageHeader>

    <AuthenticatorsEdit />
  </DocumentTitle>
);

export default AuthenticatorsEditPage;
