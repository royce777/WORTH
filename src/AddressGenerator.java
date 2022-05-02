public class AddressGenerator{ // generates consequent ip addresses for multicast
    private int part1;
    private int part2;
    private int part3;
    private int part4;

    public AddressGenerator(int part1, int part2, int part3, int part4){
        this.part1 = part1;
        this.part2 = part2;
        this.part3 = part3;
        this.part4 = part4;
    }

    public String generate(){
        if(part4<255){
            this.part4++;
            return part1 + "." + part2 + "." + part3 + "." + part4;
        }
        else{
            if(part3<255){
                this.part3++;
                this.part4=0;
                return part1 + "." + part2 + "." + part3 + "." + part4;
            }
            else{
                if(part2<255){
                    this.part2++;
                    this.part3=0;
                    this.part4=0;
                    return part1 + "." + part2 + "." + part3 + "." + part4;
                }
                else{
                    if(part1<239){
                        this.part1++;
                        this.part2=0;
                        this.part3=0;
                        this.part4=0;
                        return part1 + "." + part2 + "." + part3 + "." + part4;
                    }
                    else return "ERROR";
                }
            }
        }
    }

    public void reset(){
        part1 = 224;
        part2 = 0;
        part3 = 0;
        part4 = 0;
    }
}
