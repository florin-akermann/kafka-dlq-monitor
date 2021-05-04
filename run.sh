for i in {2..100000}
do
    echo producing $i
    echo $i | kafkacat -b localhost:9092 -P -t in
done