
interface Request {
  endpoint: string;
  method: string;
  arguments: any[];
}

export interface LinterEndpoint {

  start(process): void;

}