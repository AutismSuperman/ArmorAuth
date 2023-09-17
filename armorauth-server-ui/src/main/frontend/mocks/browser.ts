import { setupWorker } from 'msw';
import { handlers, defaultHandlers } from './login';

export const mocker = setupWorker(...handlers, ...defaultHandlers);
