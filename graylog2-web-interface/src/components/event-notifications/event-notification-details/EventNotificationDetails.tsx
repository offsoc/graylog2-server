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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { ReadOnlyFormGroup } from 'components/common';
import { Alert } from 'components/bootstrap';
import SectionComponent from 'components/common/Section/SectionComponent';
import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';

const _getNotificationPlugin = (type: string) => {
  if (type === undefined) {
    return undefined;
  }

  return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type);
};

type EventNotificationDetailsProps = {
  notification: EventNotification;
};

const EventNotificationDetails = ({ notification }: EventNotificationDetailsProps) => {
  const notificationPlugin = _getNotificationPlugin(notification.config.type);
  const DetailsComponent = notificationPlugin?.detailsComponent;

  return (
    <SectionComponent title="Details">
      <ReadOnlyFormGroup label="Title" value={notification.title} />
      <ReadOnlyFormGroup label="Description" value={notification.description} />
      <ReadOnlyFormGroup label="Notification Type" value={notification.config.type} />
      {DetailsComponent ? (
        <DetailsComponent notification={notification} />
      ) : (
        <Alert bsStyle="danger">Notification type not supported</Alert>
      )}
    </SectionComponent>
  );
};

export default EventNotificationDetails;
