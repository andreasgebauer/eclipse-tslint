import { LinterEndpoint } from "linterEndpoint";
import * as readline from 'readline';

export class LinterStdIOEndpoint implements LinterEndpoint {

  start(process): void {
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
      var request: Request = JSON.parse( requestJson );

      // invoke the endpoint method with the supplied arguments
      var method = this.endpoint[request.method];
      var result = method.apply( this.endpoint, request.arguments );

      // convert undefined to null (its basically the Java equivalent of void)
      if ( result === undefined ) {
        result = null;
      }

      // convert the result to JSON and write it to stdout
      var resultJson = JSON.stringify( result );
      console.log( "RESULT: " + resultJson );
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


