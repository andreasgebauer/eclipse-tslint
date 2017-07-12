import { LinterEndpoint } from './linterEndpoint';

import * as socketIO from 'socket.io';


export class LinterSocketEndpoint implements LinterEndpoint {

  private sio;

  start(): void {

    this.sio = socketIO();

    // listen for a connection
    this.sio.on( 'connection', function( socket ) {
      console.log( 'User ' + socket.id + ' connected' );
    } );
  }

}
