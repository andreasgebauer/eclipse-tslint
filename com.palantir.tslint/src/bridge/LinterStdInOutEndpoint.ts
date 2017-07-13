import { LinterEndpoint, Request } from "linterEndpoint";
import * as readline from 'readline';
import { LinterManager } from "./LinterManager";


export class LinterStdIOEndpoint implements LinterEndpoint {

  constructor( private linterManager: LinterManager ) {
  }

  start( process: NodeJS.Process ): void {
    var myProcess: any = process;
    var rl = readline.createInterface( myProcess.stdin, myProcess.stdout );

    // process incoming requests from stdin
    rl.on( "line", ( line: string ) => {
      this.processRequest( line );
    } );

    // exit when stdin is closed
    rl.on( "close", () => {
      myProcess.exit( 0 );
    } );
  }

  private processRequest( requestJson: string ) {
    try {
      const result = this.linterManager.processRequest( requestJson );
      console.log( "RESULT: " + result );
    } catch ( e ) {
      var error: string;

      if ( e.stack != null ) {
        error = e.stack;
      } else if ( e.message != null ) {
        error = e.message;
      } else {
        error = "Error: No stack trace or error message was provided.";
      }

      console.log( "ERROR: " + error.replace( /\n/g, "\\n" ) );
    }
  }
}


