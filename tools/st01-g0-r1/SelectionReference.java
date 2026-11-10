public class SelectionReference {
 static String f(long seed,int x,int z) {
  long h=seed ^ ((((long)x & 0xffffffffL)<<32)|((long)z & 0xffffffffL));
  h^=h>>>33;h*=0xff51afd7ed558ccdL;h^=h>>>33;h*=0xc4ceb9fe1a85ec53L;h^=h>>>33;
  return (h&1L)==0 ? "EAST":"WEST";
 }
 public static void main(String[] args) {
  System.out.println(f(0,0,0));System.out.println(f(1001,16,32));System.out.println(f(1002,-320,400));
  System.out.println(f(1003,-1,-1));System.out.println(f(1004,Integer.MAX_VALUE,Integer.MIN_VALUE));
 }
}
