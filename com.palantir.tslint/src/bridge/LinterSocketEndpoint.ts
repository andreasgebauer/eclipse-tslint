
import * as net from 'net';

import { LinterEndpoint } from './linterEndpoint';
import { LinterManager } from './LinterManager';
import { Logger } from './Logger';

export class LinterSocketEndpoint implements LinterEndpoint {

  private logger = new Logger(LinterSocketEndpoint);
  
  private clients: net.Socket[] = [];

  constructor( private linterManager: LinterManager ) {
  }

  start(): void {

    net.createServer(( socket ) => {
      const name = socket.remoteAddress + ':' + socket.remotePort;

      this.clients.push( socket );

      console.log( 'User ' + name + ' connected' );

      socket.on( 'connect', () => {
        console.log( 'connect', name );
      } );

      socket.on( 'data', ( data ) => {
        try {
          const result = this.linterManager.processRequest( data + '' );
          socket.write( 'RESULT: ' );
          socket.write( result );
          socket.write( '\n' );
        
          socket.end();
          socket.destroy();

        } catch ( err ) {
          console.error( 'error processing message ' + data + '. ' + err );
        }
      } );

      socket.on( 'close', ( error ) => {
        console.log( 'disconnect', error );
      } );

    } ).listen( 12345 );

  }

}
