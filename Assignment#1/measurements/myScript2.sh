echo Client > throughput_L$1.txt
cat throughput_L$1_C.txt >> throughput_L$1.txt 
echo Server >> throughput_L$1.txt 
cat throughput_L$1_S.txt >> throughput_L$1.txt
