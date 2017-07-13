
export class Logger {

  private name: string;
  constructor( name: string | any ) {
    if ( typeof name === 'string' ) {
      this.name = name;
    } else {
      this.name = typeof name;
    }
  }

  log( msg: string, ...args: any[] ) {
    const date: Date = new Date();

    console.log( date.toTimeString() + ' ' + this.name.toString() + ' ' + msg, args );
  }
}