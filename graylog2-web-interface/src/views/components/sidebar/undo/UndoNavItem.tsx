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
import React, { useCallback } from 'react';

import NavItem from 'views/components/sidebar/NavItem';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useViewsSelector from 'views/stores/useViewsSelector';
import { selectUndoRedoAvailability } from 'views/logic/slices/undoRedoSelectors';
import { undo } from 'views/logic/slices/undoRedoActions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useHotkey from 'hooks/useHotkey';
import type { ViewType } from 'views/logic/views/View';
import useViewType from 'views/hooks/useViewType';

const TITLE = 'Undo';

const UndoNavItem = ({ sidebarIsPinned }: { sidebarIsPinned: boolean }) => {
  const viewType = useViewType();
  const dispatch = useViewsDispatch();
  const { isUndoAvailable } = useViewsSelector(selectUndoRedoAvailability);
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();

  const onClick = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_SIDEBAR_UNDO, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'search-sidebar-undo',
    });

    return dispatch(undo());
  }, [dispatch, location.pathname, sendTelemetry]);

  useHotkey({
    actionKey: 'undo',
    callback: () => dispatch(undo()),
    scope: viewType.toLowerCase() as Lowercase<ViewType>,
  });

  return (
    <NavItem
      disabled={!isUndoAvailable}
      onClick={onClick}
      icon="undo"
      title={TITLE}
      ariaLabel={TITLE}
      sidebarIsPinned={sidebarIsPinned}
    />
  );
};

export default UndoNavItem;
